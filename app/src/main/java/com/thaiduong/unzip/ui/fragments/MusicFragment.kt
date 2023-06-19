package com.thaiduong.unzip.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentMusicBinding
import com.thaiduong.unzip.models.Song
import com.thaiduong.unzip.services.MusicService
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.ACTION_MUSIC
import com.thaiduong.unzip.utils.ACTION_STOP_SERVICE
import com.thaiduong.unzip.utils.AppUtils.getSongFromPhone
import com.thaiduong.unzip.utils.SONG_STATUS
import com.thaiduong.unzip.utils.STOP_SERVICE
import java.io.File

class MusicFragment(override val layoutId: Int = R.layout.fragment_music) :
    BaseFragment<FragmentMusicBinding>() {

    private lateinit var path: String
    private lateinit var playIntent: Intent
    private var songService: MusicService? = null
    private var isStopService = false

    private var songList = mutableListOf<Song>()
    private var currentPosition = -1
    private var isConnection = false

    private var mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_MUSIC == intent?.action) {
                val isPlayMusic = intent.getStringExtra(SONG_STATUS).toString()

                if (isPlayMusic == "play")
                    requireActivity().findViewById<ImageView>(R.id.imvPlay)
                        .setImageResource(R.drawable.ic_pause)
                if (isPlayMusic == "pause")
                    requireActivity().findViewById<ImageView>(R.id.imvPlay)
                        .setImageResource(R.drawable.ic_play)
            }
        }
    }

    private var mBroadcastService = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_STOP_SERVICE == intent?.action) {
                isStopService = intent.getBooleanExtra(STOP_SERVICE, false)
            }
        }
    }

    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: MusicService.SongBinder = service as MusicService.SongBinder
            songService = binder.getService()
            songService?.setSongName(binding.tvName)
            songService?.initializeSeekBar(
                requireActivity().findViewById(R.id.seekBarTime),
                binding.tvTimeCount,
                binding.tvFullTime
            )
            isConnection = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnection = false
        }

    }

    private val requestPermissionsBelowAndroid11 = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all {
            it.value
        }
        if (granted) {
            songList = requireActivity().getSongFromPhone(File(path).parentFile!!.name)
            initMusic()
        }
    }

    private val requestPermissionsAboveAndroid11 = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        songList = requireActivity().getSongFromPhone(File(path).parentFile!!.name)
        initMusic()
    }

    companion object {
        fun newInstance(path: String): MusicFragment {
            val args = Bundle()
            args.putString("path", path)
            val fragment = MusicFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun initUi() {
        initService()
        path = arguments?.getString("path").toString()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            requestPermissionsBelowAndroid11.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        else requestPermissionsAboveAndroid11.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    }

    override fun doWork() {
        binding.imvLooping.setOnClickListener {
            if (songService?.getLooping() == true) {
                songService?.playLooping(false)
                songService?.isLooping = false
                binding.imvLooping.setImageResource(R.drawable.ic_loop_unselected)
            } else {
                songService?.playLooping(true)
                songService?.isLooping = true
                binding.imvLooping.setImageResource(R.drawable.ic_loop_selected)
            }
        }
        binding.imvPrevious.setOnClickListener {
            binding.imvPlay.setImageResource(R.drawable.ic_pause)
            songService?.playPrevious()
            rotateImageMusic()
        }
        binding.imvNext.setOnClickListener {
            binding.imvPlay.setImageResource(R.drawable.ic_pause)
            songService?.playNext()
            rotateImageMusic()
        }
        binding.imvPlay.setOnClickListener {
            if (songService?.isPlaying() == true) {
                binding.imvPlay.setImageResource(R.drawable.ic_play)
                songService?.pausePlayer()
            } else {
                binding.imvPlay.setImageResource(R.drawable.ic_pause)
                songService?.playSong()
            }
            rotateImageMusic()
        }
        binding.imvShare.setOnClickListener { songService?.shareSong(requireContext()) }
    }

    @SuppressLint("LogNotTimber")
    private fun initService() {
        try {
            playIntent = Intent(requireContext(), MusicService::class.java)
            try {
                requireActivity().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
                requireActivity().startService(playIntent)
            } catch (ignored: RuntimeException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    requireActivity().startForegroundService(playIntent)
                else
                    requireActivity().startService(playIntent)
            }
        } catch (e: Exception) {
            Log.e("MusicFragment", "initService: $playIntent")
        }
        songService = MusicService()
    }

    private fun initMusic() {
        songService?.setSongList(songList)
        for (i in songList.indices) {
            if (songList[i].aPath == File(path).absolutePath) {
                currentPosition = i
                songService?.setCurrentPosition(currentPosition)
                break
            }
        }
        songService?.playSong()
        songService?.setSongName(binding.tvName)
        rotateImageMusic()
    }

    private fun rotateImageMusic() {
        val rotateAnimation: Animation =
            AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_image_song)
        if (songService?.isPlaying() == true) {
            binding.imvMusic.startAnimation(rotateAnimation)
        } else {
            binding.imvMusic.clearAnimation()
        }

    }

    override fun onStart() {
        super.onStart()
        val mIntentFilter = IntentFilter(ACTION_MUSIC)
        mContext?.registerReceiver(mBroadcastReceiver, mIntentFilter)
        val mIntentFile = IntentFilter(ACTION_STOP_SERVICE)
        mContext?.registerReceiver(mBroadcastService, mIntentFile)
    }

    override fun onDestroy() {
        super.onDestroy()
        mContext?.unregisterReceiver(mBroadcastReceiver)
        if (isStopService) {
            mContext?.unbindService(musicConnection)
            isConnection = false
            isStopService = false
            mContext?.unregisterReceiver(mBroadcastService)
        }
    }

}