package com.thaiduong.unzip.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.thaiduong.unzip.R
import com.thaiduong.unzip.models.Song
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.utils.*
import kotlinx.coroutines.Runnable
import kotlin.system.exitProcess

class MusicService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private val soundBind: IBinder = this.SongBinder()
    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var song: Song? = null
    private var songList: ArrayList<Song> = arrayListOf()
    private var tvSongName: TextView? = null
    private var currentPosition = -1
    private var seekBarPosition = 0
    var isLooping = false
    private var handler = Handler(Looper.getMainLooper())
    private var isUnbind = false

    private var audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (mediaPlayer != null) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (mediaPlayer!!.isPlaying) {
                        go()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (mediaPlayer!!.isPlaying) {
                        pausePlayer()
                        mediaPlayer!!.stop()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (mediaPlayer!!.isPlaying) {
                        pausePlayer()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (mediaPlayer!!.isPlaying) {
                        pausePlayer()
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                    if (mediaPlayer!!.isPlaying) {
                        go()
                    }
                }
            }
        }
    }

    private var audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(audioFocusChangeListener)
                build()
            }
        } else null

    companion object {
        private const val ACTION_CLOSE = 0
        private const val ACTION_PLAY_OR_PAUSE = 1
        private const val ACTION_PREVIOUS = 2
        private const val ACTION_NEXT = 3
    }

    inner class SongBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder {
        initMusicPlayer()
        return soundBind
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionPlayer = intent?.getIntExtra(ACTION_PLAYER, -1)
        handlerAction(actionPlayer)
        return START_NOT_STICKY
    }

    private fun initMusicPlayer() {
        mediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        )
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnErrorListener(this)
    }

    private fun handlerAction(actionPlayer: Int?) {
        when (actionPlayer) {
            ACTION_CLOSE -> {
                pausePlayer()
                val intentMusic = Intent(ACTION_MUSIC)
                intentMusic.putExtra(SONG_STATUS, "pause")
                sendBroadcast(intentMusic)
                notificationManager?.cancelAll()
                if (!isOpenApp()) {
                    if (!isUnbind) {
                        val intentService = Intent(ACTION_STOP_SERVICE)
                        intentService.putExtra(STOP_SERVICE, true)
                        sendBroadcast(intentService)
                    }
                    stopSelf()
                }
            }
            ACTION_PREVIOUS -> {
                playPrevious()
            }
            ACTION_NEXT -> {
                playNext()
            }
            ACTION_PLAY_OR_PAUSE -> {
                playOrPause()
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotificationChannel(songName: String): Notification {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Vibrator"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, ReadFilesActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TaskStackBuilder.create(this).run {
                addNextIntentWithParentStack(notificationIntent)
                getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        } else {
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setCustomContentView(createCollapsedNotificationView(songName))
            .setCustomBigContentView(createExpandedNotificationView(songName))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
        notificationManager?.notify(FOREGROUND_SERVICE, notification)
        return notification as Notification
    }

    private fun createCollapsedNotificationView(songName: String): RemoteViews {
        return RemoteViews(this.packageName, R.layout.collapsed_notification).apply {
            setOnClickPendingIntent(
                R.id.imvPrevious, getPendingIntent(this@MusicService, ACTION_PREVIOUS)
            )
            setOnClickPendingIntent(
                R.id.imvPlay, getPendingIntent(this@MusicService, ACTION_PLAY_OR_PAUSE)
            )
            setOnClickPendingIntent(
                R.id.imvNext, getPendingIntent(this@MusicService, ACTION_NEXT)
            )
            if (isPlaying()) setImageViewResource(R.id.imvPlay, R.drawable.ic_notify_pause)
            else setImageViewResource(R.id.imvPlay, R.drawable.ic_notify_play)
            setTextViewText(R.id.tvSongName, songName)
        }
    }

    private fun createExpandedNotificationView(songName: String): RemoteViews {
        return RemoteViews(this.packageName, R.layout.expanded_notification).apply {
            setOnClickPendingIntent(
                R.id.imvPrevious, getPendingIntent(this@MusicService, ACTION_PREVIOUS)
            )
            setOnClickPendingIntent(
                R.id.imvPlay, getPendingIntent(this@MusicService, ACTION_PLAY_OR_PAUSE)
            )
            setOnClickPendingIntent(
                R.id.imvNext, getPendingIntent(this@MusicService, ACTION_NEXT)
            )
            setOnClickPendingIntent(
                R.id.imvClose, getPendingIntent(this@MusicService, ACTION_CLOSE)
            )
            if (isPlaying()) setImageViewResource(R.id.imvPlay, R.drawable.ic_notify_pause)
            else setImageViewResource(R.id.imvPlay, R.drawable.ic_notify_play)
            setTextViewText(R.id.tvSongName, songName)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntent(context: Context, action: Int): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_PLAYER, action)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(
                context.applicationContext,
                action,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context.applicationContext,
                action,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private fun requestAudioFocus(): Boolean {
        var res: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                build()
            }
        }
        try {
            if (audioFocusRequest != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { res = audioManager.requestAudioFocus(it) }
                }
            } else {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                    res = audioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "requestAudioFocus: $audioFocusRequest")
        }
        return res == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun go() {
        mediaPlayer?.start()
    }

    fun isPlaying(): Boolean {
        var isPlay = false
        try {
            if (mediaPlayer != null) {
                isPlay = mediaPlayer!!.isPlaying
            }
        } catch (e: Exception) {
            Log.e("MusicService", "isPlaying: $mediaPlayer")
        }
        return isPlay
    }

    fun getLooping(): Boolean {
        return mediaPlayer?.isLooping!!
    }

    fun playLooping(isLooping: Boolean) {
        mediaPlayer?.isLooping = isLooping
    }

    fun playNext() {
        if (song == null) return
        seekBarPosition = 0
        song?.isPlay = false
        do {
            currentPosition++
            if (currentPosition >= songList.size) currentPosition = 0
            else break
        } while (true)
        playSong()
    }

    fun playPrevious() {
        if (song == null) return
        seekBarPosition = 0
        song?.isPlay = false
        do {
            currentPosition--
            if (currentPosition < 0) currentPosition = songList.lastIndex
            else break
        } while (true)
        playSong()
    }

    private fun playOrPause() {
        val intent = Intent(ACTION_MUSIC)
        if (isPlaying()) {
            pausePlayer()
            intent.putExtra(SONG_STATUS, "pause")
        } else {
            playSong()
            intent.putExtra(SONG_STATUS, "play")
        }
        sendBroadcast(intent)
    }

    fun pausePlayer() {
        mediaPlayer?.pause()
        song?.isPlay = false
        createNotificationChannel(tvSongName?.text.toString())
        seekBarPosition = mediaPlayer?.currentPosition!!
    }

    fun playSong() {
        if (requestAudioFocus()) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer()
                mediaPlayer?.isLooping = isLooping
            } catch (e: Exception) {
                Log.e("MusicService", "playSong: $mediaPlayer")
            }

            val intent = Intent(ACTION_MUSIC)
            intent.putExtra(SONG_STATUS, "play")
            intent.putExtra(SONG_POSITION, currentPosition)
            sendBroadcast(intent)

            song = songList[currentPosition]
            mediaPlayer = MediaPlayer.create(this, Uri.parse(song?.aPath))
            mediaPlayer?.seekTo(seekBarPosition)
            mediaPlayer?.start()
            for (mSong in songList) {
                mSong.isPlay = mSong == song
            }
            tvSongName?.text = song?.aName
            createNotificationChannel(tvSongName?.text.toString())
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.reset()
                if (isLooping) playSong()
                else playNext()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initializeSeekBar(seekBar: SeekBar, start: TextView, end: TextView) {
        var runnable: Runnable? = null
        runnable = Runnable {
            try {
                seekBar.max = getDuration()
                seekBar.progress = getCurrentDuration()
                val diff = getDuration() - getCurrentDuration()
                end.text = FileFormat.timeFormat(diff)
                start.text = FileFormat.timeFormat(getCurrentDuration())
                runnable?.let { handler.postDelayed(it, 100) }
            } catch (e: IllegalStateException) {
                Log.e("MusicService", "initializeSeekBar: failed")
            }
        }
        handler.postDelayed(runnable, 100)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    mediaPlayer!!.seekTo(i * 100)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.e("MusicService", "onStartTrackingTouch: $seekBar")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.e("MusicService", "onStopTrackingTouch: $seekBar")
            }

        })
    }

    private fun getCurrentDuration(): Int {
        var duration = 0
        try {
            duration = mediaPlayer?.currentPosition?.div(100)!!
        } catch (e: Exception) {
            playNext()
        }
        return duration
    }

    private fun getDuration(): Int {
        var duration = 0
        try {
            duration = mediaPlayer?.duration?.div(100)!!
        } catch (e: Exception) {
            playNext()
        }
        return duration
    }

    fun setSongName(textView: TextView) {
        tvSongName = textView
    }

    fun setSongList(songList: List<Song>) {
        this.songList = songList as ArrayList<Song>
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
    }

    fun startForeground() {
        startForeground(
            FOREGROUND_SERVICE,
            createNotificationChannel(tvSongName?.text.toString())
        )
        seekBarPosition = mediaPlayer?.currentPosition!!
    }

    private fun isOpenApp(): Boolean {
        val application = this.applicationContext
        val activityManager: ActivityManager =
            this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcessList = activityManager.runningAppProcesses
        if (runningProcessList != null) {
            val myApp = runningProcessList.find { it.processName == application.packageName }
            ActivityManager.getMyMemoryState(myApp)
            return myApp?.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
        return false
    }


    fun shareSong(context: Context) {
        AppUtils.shareFile(context, mutableListOf(song?.aPath.toString()), "audio",)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        notificationManager?.cancelAll()
        stopSelf()
        Log.e("SoundService", "onTaskRemoved: $rootIntent")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e("SoundService", "onUnbind: $intent")
        isUnbind = true
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        mediaPlayer?.release()
        audioFocusRequest = null
        seekBarPosition = 0
        Log.e("MusicService", "onDestroy")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            exitProcess(0)
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.e("SoundService", "onPrepared")
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.e("SoundService", "onCompletion")
    }

}