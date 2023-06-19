package com.thaiduong.unzip.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.thaiduong.unzip.App
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityMainBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.models.SettingModel
import com.thaiduong.unzip.services.MusicService
import com.thaiduong.unzip.ui.adapters.CollapseFolderAdapter
import com.thaiduong.unzip.ui.adapters.ExpandedFolderAdapter
import com.thaiduong.unzip.ui.adapters.MenuAdapter
import com.thaiduong.unzip.ui.bases.BaseActivity
import com.thaiduong.unzip.utils.*
import com.thaiduong.unzip.utils.AppUtils.contact
import com.thaiduong.unzip.utils.AppUtils.getFile
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.AppUtils.reviewApp
import com.thaiduong.unzip.utils.AppUtils.shareApp
import com.thaiduong.unzip.utils.customclass.BottomSheetFunction
import com.thaiduong.unzip.utils.interfaces.GetBottomSheetState
import com.thaiduong.unzip.utils.interfaces.IGetBottomSheetLayoutGravity
import com.thaiduong.unzip.utils.interfaces.IItemSelected
import com.thaiduong.unzip.utils.interfaces.IShowRecentlyFile
import java.io.File

class MainActivity(override val layoutId: Int = R.layout.activity_main) :
    BaseActivity<ActivityMainBinding>(), IShowRecentlyFile, IItemSelected,
    IGetBottomSheetLayoutGravity, GetBottomSheetState {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadExternalStoragePermission = false
    private var isWriteExternalStoragePermission = false
    private var isManageExternalStoragePermission = false

    private var songService: MusicService? = null
    private lateinit var playIntent: Intent
    private var isConnection: Boolean = false
    private var isStopService = false

    private var adapter: MenuAdapter? = null
    private var fileRecentlyList = mutableListOf<FolderOrFile>()
    private var filesAndFolders: Array<File>? = null
    private var titleList: List<String>? = null
    private var mExpandedFolderAdapter = ExpandedFolderAdapter(this)

    private var mBottomSheetFunction: BottomSheetFunction? = null

    private lateinit var params: ConstraintLayout.LayoutParams


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
        checkPermission()
        initMenu()
        params = binding.h2.layoutParams as ConstraintLayout.LayoutParams
        if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
            initService()
            initAdapter()
            initFolder()
            filesAndFolders?.let { initRecentlyAdapter(it, fileRecentlyList, App.dataStore.getBoolean(
                IS_SHOW_FILE_RECENTLY, true)) }
        }
    }

    @SuppressLint("InflateParams")
    override fun doWork() {
        binding.imvMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            else
                binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.imvSearch.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
                startActivity(Intent(this, SearchActivity::class.java))
            else openSettings()
        }
        binding.imvExpanded.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
                val path = myGetExternalStorageDir()
                menuSelectedScreen(path!!, getString(R.string.internal_storage))
            } else openSettings()

        }
        binding.navigationVewExpanded.setOnGroupExpandListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
                if (it == 0) {
                    val path = "${this.myGetExternalStorageDir()}/.Recycle Bin"
                    menuSelectedScreen(path, getString(R.string.recycle_bin))
                }
            } else openSettings()
        }
        binding.navigationVewExpanded.setOnGroupCollapseListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
                if (it == 0) {
                    val path = "${this.myGetExternalStorageDir()}/.Recycle Bin"
                    menuSelectedScreen(path, getString(R.string.recycle_bin))
                }
            } else openSettings()

        }
        binding.linearExtracted.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
                val path = "${this.myGetExternalStorageDir()}/.Extracted"
                menuSelectedScreen(path, getString(R.string.extracted))
            } else openSettings()
        }
        binding.linearCompressed.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
                val path = "${this.myGetExternalStorageDir()}"
                menuSelectedScreen(path, getString(R.string.Compressed))
            } else openSettings()
        }
        binding.linearWifiTransfer.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
                startActivity(Intent(this, WifiTransferActivity::class.java))
            else openSettings()
        }
        binding.linearFileExplorer.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
                menuSelectedScreen(CATEGORY_NAME, getString(R.string.file_explorer))
            else openSettings()
        }
        binding.linearDropbox.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
                startActivity(Intent(this, DropboxActivity::class.java))
            else openSettings()
        }
        binding.linearGoogleDriver.setOnClickListener {
            Toast.makeText(this, getString(R.string.google_drive_notify), Toast.LENGTH_SHORT).show()
        }
        binding.tvSeeAll.setOnClickListener {
            if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
                menuSelectedScreen(CATEGORY_NAME, getString(R.string.recently))
            else openSettings()
        }

        binding.navigationVewExpanded.setOnChildClickListener { _, _, _, childPosition, _ ->
            when (childPosition) {
                1 -> {
                    val dialog = Dialog(this, R.style.DialogStyle)
                    dialog.setContentView(
                        LayoutInflater.from(this).inflate(R.layout.changelang_dialog, null, false)
                    )
                    when (App.dataStore.getString("lang", "")) {
                        "en" -> {
                            dialog.findViewById<ImageView>(R.id.imvEnglishSelected)
                                .setImageResource(R.drawable.ic_selected)
                            dialog.findViewById<ImageView>(R.id.imvVietnameseSelected)
                                .setImageResource(R.drawable.ic_unselected)
                        }
                        "vi" -> {
                            dialog.findViewById<ImageView>(R.id.imvEnglishSelected)
                                .setImageResource(R.drawable.ic_unselected)
                            dialog.findViewById<ImageView>(R.id.imvVietnameseSelected)
                                .setImageResource(R.drawable.ic_selected)
                        }
                    }
                    dialog.setCancelable(true)
                    dialog.findViewById<LinearLayout>(R.id.btn_en).setOnClickListener {
                        if (App.dataStore.getString("lang", "en") == "en") {
                            return@setOnClickListener
                        }
                        App.dataStore.putString("lang", "en")
                        restartActivity()
                    }
                    dialog.findViewById<LinearLayout>(R.id.btn_vn).setOnClickListener {
                        if (App.dataStore.getString("lang", "vi") == "vi") {
                            return@setOnClickListener
                        }
                        App.dataStore.putString("lang", "vi")
                        restartActivity()
                    }
                    dialog.show()
                }
                2 -> menuSelectedScreen(MENU_SETTINGS, getString(R.string.privacy_policy))
                3 -> menuSelectedScreen(MENU_SETTINGS, getString(R.string.term))
                4 -> this.contact()
                5 -> this.shareApp()
                6 -> this.reviewApp()
            }
            false
        }
    }

    private fun restartActivity() {
        val intent = this.intent
        this.overridePendingTransition(0, 0)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.finish()
        this.overridePendingTransition(0, 0)
        this.startActivity(intent)
    }

    private fun checkPermission() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isReadExternalStoragePermission =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                        ?: isReadExternalStoragePermission
                isWriteExternalStoragePermission =
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                        ?: isWriteExternalStoragePermission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    isManageExternalStoragePermission =
                        permissions[Manifest.permission.MANAGE_EXTERNAL_STORAGE]
                            ?: isManageExternalStoragePermission
            }
        requestPermission()
    }

    private fun requestPermission() {
        isReadExternalStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        isWriteExternalStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isManageExternalStoragePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        val permissionRequest: MutableList<String> = ArrayList()
        if (!isReadExternalStoragePermission) {
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!isWriteExternalStoragePermission) {
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!isManageExternalStoragePermission) {
                permissionRequest.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }
        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }

    private fun initMenu() {
        val listData = SettingModel.getData(this)
        titleList = ArrayList(listData.keys)
        adapter = MenuAdapter(this, titleList as ArrayList<String>, listData)
        binding.navigationVewExpanded.setGroupIndicator(null)
        binding.navigationVewExpanded.divider = null
        binding.navigationVewExpanded.setAdapter(adapter)
        binding.navigationVewExpanded.expandGroup(1)
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
            Log.e("MainActivity", "initService: $playIntent")
        }
        songService = MusicService()
    }

    private fun initAdapter() {
        val path = this.myGetExternalStorageDir()
        val root = path?.let { File(it) }
        filesAndFolders = root?.listFiles() as Array<File>
        if (filesAndFolders?.isEmpty() == true) {
            return
        }
        filesAndFolders?.sortByDescending { it.lastModified() }
        //Internal Storage
        val arr = mutableListOf<FolderOrFile>()
        arr.clear()
        for (mFile in filesAndFolders!!) {
            if (arr.size >= 4) break
            if (mFile.name.first() != '.') {
                arr.add(FolderOrFile(mFile))
            }
        }
        val adapter = CollapseFolderAdapter(this)
        adapter.submitList(arr)
        binding.rcvInternalFolder.adapter = adapter
    }

    private fun initFolder() {
        var folder = File(this.myGetExternalStorageDir(), ".Recycle Bin")
        if (!folder.exists()) {
            folder.mkdir()
        }
        folder = File(this.myGetExternalStorageDir(), ".Extracted")
        if (!folder.exists()) {
            folder.mkdir()
        }
        folder = File(this.myGetExternalStorageDir(), ".Compressed")
        if (!folder.exists()) {
            folder.mkdir()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCategoryAmount() {
        //Amount file compressed
        var path = this.myGetExternalStorageDir()
        val rootFile = path?.let { File(it).listFiles() }
        val compressedFilesList = mutableListOf<FolderOrFile>()
        if (rootFile != null) {
            for (file in rootFile)
                FileManager.getFile(compressedFilesList, listOf("zip", "rar", "7z", "tar"), file)
        }
        path = "${this.myGetExternalStorageDir()}/.Compressed"
        if (File(path).listFiles()?.isNotEmpty() == true)
            for (file in File(path).listFiles()!!) {
                compressedFilesList.add(FolderOrFile(file))
            }
        var amount = compressedFilesList.size
        if (amount > 10) {
            binding.tvCompressedAmount.text = "$amount ${getString(R.string.items)}"
        } else {
            binding.tvCompressedAmount.text = "$amount ${getString(R.string.item)}"
        }
        //Amount file extracted
        path = "${this.myGetExternalStorageDir()}/.Extracted"
        if (File(path).listFiles()?.isNotEmpty() == true)
            amount = File(path).listFiles()?.size!!
        if (amount > 10) {
            binding.tvExtractedAmount.text = "$amount ${getString(R.string.items)}"
        } else {
            binding.tvExtractedAmount.text = "$amount ${getString(R.string.item)}"
        }
    }

    private fun menuSelectedScreen(path: String, folderName: String) {
        val intent = Intent(this@MainActivity, FileListActivity::class.java)
        intent.putExtra(PATH, path)
        intent.putExtra(FOLDER_NAME, folderName)
        startActivity(intent)
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun openSettings() {
        Toast.makeText(
            this@MainActivity,
            "Storage permission is requires,please allow from settings",
            Toast.LENGTH_SHORT
        ).show()
        val mBuild = android.app.AlertDialog.Builder(this)
        with(mBuild) {
            setTitle(R.string.open_permission)
            setMessage(R.string.guideTurnOnPermission)
            setCancelable(false)
            setPositiveButton("SETTING") { _, _ ->
                let {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", this@MainActivity.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    this@MainActivity.finish()
                }
            }
            setNegativeButton("CLOSE") { dialog, _ -> dialog.dismiss() }
        }
        val mAlertDialog = mBuild.create()
        Handler(Looper.getMainLooper()).postDelayed({ mAlertDialog.show() }, 3000)
    }

    override fun onStart() {
        super.onStart()
        val mIntentFile = IntentFilter(ACTION_STOP_SERVICE)
        registerReceiver(mBroadcastService, mIntentFile)
    }

    override fun onStop() {
        super.onStop()
        if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission) {
            initAdapter()
            mBottomSheetFunction?.turnOffBottomSheet()
        }
    }

    private fun initRecentlyAdapter(
        filesAndFolders: Array<File>,
        arr: MutableList<FolderOrFile>,
        showFile: Boolean
    ) {
        arr.clear()
        this.getFile(filesAndFolders, arr, 5)
        mExpandedFolderAdapter.submitList(arr)
        binding.rcvRecentlyFile.adapter = mExpandedFolderAdapter
        mBottomSheetFunction = BottomSheetFunction(this, getString(R.string.recently))
        mBottomSheetFunction?.initUi()
        mBottomSheetFunction?.setData(fileRecentlyList, mExpandedFolderAdapter, null)
        binding.rcvRecentlyFile.visibility = if (showFile) View.VISIBLE else View.INVISIBLE
    }

    override fun isShowFile(isShow: Boolean) {
        if (isReadExternalStoragePermission && isWriteExternalStoragePermission || isManageExternalStoragePermission)
            filesAndFolders?.let { initRecentlyAdapter(it, fileRecentlyList, isShow) }
    }

    @SuppressLint("SetTextI18n")
    override fun selectedItem(mFolderOrFile: FolderOrFile) {
        mBottomSheetFunction?.setOnSelectedItem(mFolderOrFile)
    }

    override fun getLayoutGravity(isShow: Boolean) {
        params.guidePercent = if (isShow) 0.9f else 1f
        binding.h2.layoutParams = params
    }

    override fun getBottomSheetStateExpanded(isExpanded: Boolean) {
        mExpandedFolderAdapter.isDisableClick = isExpanded
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStopService) {
            unbindService(soundConnection)
            isConnection = false
            isStopService = false
            unregisterReceiver(mBroadcastService)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        if (mBottomSheetFunction?.setBottomSheetState() == true) {
            mBottomSheetFunction?.turnOffBottomSheet()
            return
        }
        super.onBackPressed()
    }

}