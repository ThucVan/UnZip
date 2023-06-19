package com.thaiduong.unzip.ui.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.TableRow
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityReadFilesBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.services.MusicService
import com.thaiduong.unzip.ui.bases.BaseActivity
import com.thaiduong.unzip.ui.fragments.*
import com.thaiduong.unzip.utils.*
import com.thaiduong.unzip.utils.AppUtils.moveRecycleBin
import com.thaiduong.unzip.utils.interfaces.IActionBarShow
import java.io.File

class ReadFilesActivity(override val layoutId: Int = R.layout.activity_read_files) :
    BaseActivity<ActivityReadFilesBinding>(), IActionBarShow {
    private var songService: MusicService? = null
    private lateinit var playIntent: Intent
    private var isConnection: Boolean = false
    private var isStopService = false

    private lateinit var path: String
    private var extensionFile = ""
    private lateinit var mImageFragment: ImageFragment
    private lateinit var mVideoFragment: VideoFragment

    private var mBroadcastService = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_STOP_SERVICE == intent?.action) {
                isStopService = intent.getBooleanExtra(STOP_SERVICE, false)
            }
        }
    }

    private val soundConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: MusicService.SongBinder = service as MusicService.SongBinder
            songService = binder.getService()
            isConnection = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnection = false
        }

    }

    override fun initUi() {
        path = intent.getStringExtra(PATH).toString()
        extensionFile = intent.getStringExtra(EXTENSION_FILE).toString()

        setSupportActionBar(binding.toolBar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            supportActionBar?.title = Html.fromHtml(
                "<font color=\"blue\">" + File(path).name + "</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            @Suppress("DEPRECATION")
            supportActionBar?.title =
                Html.fromHtml("<font color=\"blue\">" + File(path).name + "</font>")
        }
        binding.toolBar.setNavigationIcon(R.drawable.ic_menu_back)
        binding.toolBar.inflateMenu(R.menu.read_file_menu)

        initView()

    }

    override fun doWork() {
        binding.toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        var degrees = 90f
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.action_delete -> {
                    this.moveRecycleBin(this, null, mutableListOf(FolderOrFile(File(path))))
                }
                R.id.action_rotating -> {
                    mImageFragment.imageRotating(degrees)
                    degrees += 90f
                }
                R.id.action_share -> AppUtils.shareFile(
                    this,
                    mutableListOf(path),
                    File(path).extension
                )
                R.id.action_detail -> showFileDetail()
                R.id.action_exit -> finish()
            }
            true
        }

    }

    @SuppressLint("InflateParams")
    private fun initView() {
        when (extensionFile.lowercase()) {
            "mp3" -> {
                initService()
                replaceScreen(MusicFragment.newInstance(SingletonLastSong.getInstance(this).path!!))
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
            "mp4", "gif" -> {
                mVideoFragment = VideoFragment.newInstance(path)
                replaceScreen(mVideoFragment)
            }
            "xlsx", "docx", "pptx" -> {
                FileOpener.openFile(this, File(path))
                finish()
            }
            "zip", "rar", "7z", "tar", "lzma" -> {
                replaceScreen(ExtractFragment.newInstance(path, extensionFile))
            }
            "pdf", "txt" -> {
                replaceScreen(TextFragment.newInstance(path))
            }
            "apk" -> {
                replaceScreen(ExtractFragment.newInstance(path, extensionFile))
            }
            "jpg", "jpeg", "png", "webp" -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    supportActionBar?.hide()
                }, 5000)

                mImageFragment = ImageFragment.newInstance(path)
                replaceScreen(mImageFragment)
            }
            else -> {
                val dialog = Dialog(this, R.style.DialogStyle)
                dialog.setContentView(
                    LayoutInflater.from(this).inflate(R.layout.cannot_read_file_dialog, null, false)
                )
                dialog.setCancelable(false)
                dialog.findViewById<Button>(R.id.btn_ok).setOnClickListener {
                    dialog.dismiss()
                    finish()
                }
                dialog.show()
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun initService() {
        try {
            playIntent = Intent(this, MusicService::class.java)
            try {
                bindService(playIntent, soundConnection, Context.BIND_AUTO_CREATE)
                startService(playIntent)
            } catch (ignored: RuntimeException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(playIntent)
                } else {
                    startService(playIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("ReadFilesActivity", "initService: $playIntent")
        }
        songService = MusicService()
    }

    @SuppressLint("InflateParams", "SetTextI18n", "CutPasteId")
    private fun showFileDetail() {
        val dialog = Dialog(this, R.style.DialogStyle)
        dialog.setContentView(
            LayoutInflater.from(this).inflate(R.layout.file_detail, null, false)
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.tvFileName).text = File(path).name
        dialog.findViewById<TextView>(R.id.tvFilePath).text = path
        dialog.findViewById<TextView>(R.id.tvFileSize).text =
            FileFormat.sizeFormat(File(path).length())
        dialog.findViewById<TextView>(R.id.tvFileModification).text =
            FileFormat.dateFormat(File(path).lastModified())
        val pickedImagePath = path
        val bitMapOption = BitmapFactory.Options()
        bitMapOption.inJustDecodeBounds = true
        BitmapFactory.decodeFile(pickedImagePath, bitMapOption)
        val imageWidth = bitMapOption.outWidth
        val imageHeight = bitMapOption.outHeight
        if (extensionFile in arrayOf("jpg", "jpeg", "png", "webp")) {
            dialog.findViewById<TableRow>(R.id.tableImage).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvFileResolution).text = "$imageWidth x $imageHeight"
        }
        dialog.show()
    }

    private fun replaceScreen(mFragment: Fragment) {
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(R.id.constraintScreen, mFragment)
        fragmentTransition.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_file_menu, menu)
        when (extensionFile) {
            "jpg", "jpeg", "png", "webp" -> {
                menu.getItem(0).subMenu.getItem(4).isVisible = false
            }
            "mp3", "null" -> {
                menu.getItem(0).subMenu.getItem(1).isVisible = false
                menu.getItem(0).subMenu.getItem(3).isVisible = false
            }
            "mp4", "gif" -> {
                menu.getItem(0).subMenu.getItem(1).isVisible = false
                menu.getItem(0).subMenu.getItem(2).isVisible = false
            }
            "pdf", "txt" -> {
                menu.getItem(0).subMenu.getItem(1).isVisible = false
            }
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        val mIntentFile = IntentFilter(ACTION_STOP_SERVICE)
        registerReceiver(mBroadcastService, mIntentFile)
    }

    override fun isShowActionBar(isShow: Boolean) {
        if (isShow) {
            supportActionBar?.show()
        } else
            supportActionBar?.hide()
    }

    override fun onDestroy() {
        if (songService?.isPlaying() == true) {
            songService?.startForeground()
        }
        if (isStopService) {
            unbindService(soundConnection)
            isConnection = false
            isStopService = false
            unregisterReceiver(mBroadcastService)
        }
        super.onDestroy()
    }

}