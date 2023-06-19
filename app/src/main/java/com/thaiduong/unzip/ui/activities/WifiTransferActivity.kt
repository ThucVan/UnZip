package com.thaiduong.unzip.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.hwangjr.rxbus.RxBus
import com.hwangjr.rxbus.annotation.Subscribe
import com.hwangjr.rxbus.annotation.Tag
import com.hwangjr.rxbus.thread.EventThread
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityWifiTransferBinding
import com.thaiduong.unzip.services.WebService
import com.thaiduong.unzip.ui.bases.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.pengtao.filetransfer.Constants
import me.pengtao.filetransfer.FileModel
import me.pengtao.filetransfer.util.FileType
import me.pengtao.filetransfer.util.FileUtils
import me.pengtao.filetransfer.util.WifiUtils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.DecimalFormat

class WifiTransferActivity(override val layoutId: Int = R.layout.activity_wifi_transfer) :
    BaseActivity<ActivityWifiTransferBinding>() {

    private val FILE_FETCH_CODE = 2
    lateinit var mFileModelList: ArrayList<FileModel>
    private var mAlreadyWrited = ""

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Timber.tag("Internet").e("NetworkCapabilities.TRANSPORT_WIFI")
                true
            } else {
                Timber.tag("Internet").e("TRANSPORT_WIFI Fails!!!")
                false
            }
        }
        return false
    }

    override fun initUi() {
        binding.swOnOff.isChecked = WebService.isOnline
        if (binding.swOnOff.isChecked){
            showAlertIsOnline()
        }else{
            binding.alertText.text = getString(R.string.textAlertwftransferoff)
            binding.tvShowftp.visibility = View.GONE
        }
    }

    override fun doWork() {
        binding.imvBack.setOnClickListener {
            finish()
        }

        binding.btnOpenSetting.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        binding.swOnOff.setOnCheckedChangeListener { _, bol ->
            run {
                if (bol) {
                    showAlertIsOnline()
                    WebService.start(this)
                } else {
                    binding.alertText.text = getString(R.string.textAlertwftransferoff)
                    binding.tvShowftp.visibility = View.GONE
                    WebService.stop(this)
                }
            }
        }
    }

    @Subscribe(thread = EventThread.IO, tags = [Tag(Constants.RxBusEventType.LOAD_BOOK_LIST)])
    fun loadFileList(type: Int?) {
        Timber.tag("hix").e("loading")
        val dir = Constants.DIR
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()
            FileUtils.sortWithLastModified(files)
            mFileModelList.clear()
            for (file in files) {
                Timber.tag("hix").e(file.absolutePath)
                handleFiles(file.absolutePath, file.length())
            }
        }
        runOnUiThread {

        }
    }

    private fun handleFiles(path: String, length: Long) {
        val fileModel = FileModel()
        val pm: PackageManager = packageManager
        val info = pm.getPackageArchiveInfo(path, 0)
        if (info != null) {
            val appInfo = info.applicationInfo
            appInfo.sourceDir = path
            appInfo.publicSourceDir = path
            val packageName = appInfo.packageName
            val version = info.versionName
            val icon = pm.getApplicationIcon(appInfo)
            var appName: String? = pm.getApplicationLabel(appInfo).toString()
            if (TextUtils.isEmpty(appName)) {
                appName = getApplicationName(packageName)
            }
            fileModel.name = appName
            fileModel.packageName = packageName
            fileModel.path = path
            fileModel.size = getFileSize(length)
            fileModel.version = version
            fileModel.icon = icon
            fileModel.fileType = FileType.TYPE_APK
            fileModel.isInstalled = isAvailable(this, packageName)
            mFileModelList.add(fileModel)
        } else {
            fileModel.fileType = FileUtils.getFileType(path)
            fileModel.path = path
            val pathItems =
                path.split(File.separator.toRegex()).toTypedArray()
            fileModel.name = pathItems[pathItems.size - 1]
            fileModel.size = getFileSize(length)
            val icon: Drawable = ContextCompat.getDrawable(this, FileUtils.getFileTypeIcon(path))!!
            fileModel.icon = icon
            mFileModelList.add(fileModel)
        }
    }

    @Synchronized
    fun getIconFromPackageName(packageName: String, context: Context): Drawable? {
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(packageName, 0)
            val otherAppCtx = context.createPackageContext(
                packageName,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val displayMetrics: MutableList<Int> = ArrayList()
            displayMetrics.add(DisplayMetrics.DENSITY_XXXHIGH)
            displayMetrics.add(DisplayMetrics.DENSITY_XXHIGH)
            displayMetrics.add(DisplayMetrics.DENSITY_XHIGH)
            displayMetrics.add(DisplayMetrics.DENSITY_HIGH)
            displayMetrics.add(DisplayMetrics.DENSITY_TV)
            for (displayMetric in displayMetrics) {
                try {
                    val d = otherAppCtx.resources
                        .getDrawableForDensity(pi.applicationInfo.icon, displayMetric)
                    if (d != null) {
                        return d
                    }
                } catch (e: Resources.NotFoundException) { // ignore
                }
            }
        } catch (e: Exception) { // Handle Error here
        }
        val appInfo: ApplicationInfo = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return appInfo.loadIcon(pm)
    }

    private fun getApplicationName(packageName: String): String {
        var packageManager: PackageManager? = null
        var applicationInfo: ApplicationInfo?
        try {
            packageManager = applicationContext.packageManager
            applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            applicationInfo = null
        }
        return if (applicationInfo != null) {
            packageManager!!.getApplicationLabel(applicationInfo) as String
        } else packageName
    }

    private fun getFileSize(length: Long): String {
        val df = DecimalFormat("######0.0")
        if (length < 1024f) {
            return length as String + "B"
        } else if (length < 1024 * 1024f) {
            return df.format(length / 1024f.toDouble()) + "K"
        } else if (length < 1024 * 1024 * 1024f) {
            return df.format((length / 1024f / 1024f).toDouble()) + "M"
        }
        return df.format(length / 1024f / 1024f / 1024f.toDouble()) + "G"
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun isAvailable(context: Context, packageName: String?): Boolean {
        val packageManager = context.packageManager
        //Get all the APP package names of the mobile phone system, and then compare them one by one
        val pinfo = packageManager.getInstalledPackages(0)
        for (i in pinfo.indices) {
            if (pinfo[i].packageName.equals(packageName, ignoreCase = true)) {

                return true
            }
        }
        return false
    }

    @SuppressLint("LogNotTimber")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("onActivityResult", "onActivityResult")
        if (requestCode == FILE_FETCH_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            if (uri != null) {
                try {
                    val content: ContentResolver = contentResolver
                    FileUtils.copyFile(
                        content.openInputStream(data.data!!),
                        Constants.DIR.toString() + File.separator + FileUtils.getFileName(this, uri)
                    )
                    Toast.makeText(
                        this,
                        me.pengtao.filetransfer.R.string.please_refresh_web,
                        Toast.LENGTH_LONG
                    ).show()
                    RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0)

                } catch (e: IOException) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    this,
                    me.pengtao.filetransfer.R.string.read_file_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerWifiReceiver()
        isOnline(this)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.primaryClip != null) {
            val item = clipboard.primaryClip!!.getItemAt(0)
            if (item != null && item.text != null && item.text.isNotEmpty() && item.text != mAlreadyWrited) {
                CoroutineScope(Dispatchers.IO).launch {
                    RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0)
                }
            }
        }
    }

    private var mWifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isOnline(this@WifiTransferActivity)) {
                binding.imgBgWf.setImageResource(R.drawable.logo_wf_off)
                binding.swOnOff.visibility = View.GONE
                binding.btnOpenSetting.visibility = View.VISIBLE
                if( binding.btnOpenSetting.visibility == View.VISIBLE){
                    binding.alertText.text = getString(R.string.textAlertwftransferwfoff)
                }
                binding.tvShowftp.visibility = View.GONE
                binding.swOnOff.isChecked = false
            } else {
                if(!binding.swOnOff.isChecked){
                    binding.alertText.text = getString(R.string.textAlertwftransferoff)
                }
                binding.imgBgWf.setImageResource(R.drawable.logo_wftransfer_on)
                binding.swOnOff.visibility = View.VISIBLE
                binding.btnOpenSetting.visibility = View.GONE
            }
        }
    }

    private fun registerWifiReceiver() {
        val filter = IntentFilter()
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(mWifiReceiver, filter)
    }

    private fun unregisterWifiReceiver() {
        unregisterReceiver(mWifiReceiver)
    }

    override fun onPause() {
        Timber.e("onPause")
        super.onPause()
        isOnline(this)
    }

    override fun onStop() {
        super.onStop()
        Timber.e("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.e("onDestroy")
        //WebService.stop(this)
        unregisterWifiReceiver()
    }

    fun showAlertIsOnline(){
        binding.alertText.text = getString(R.string.textAlertwftransferon)
        binding.tvShowftp.visibility = View.VISIBLE
        val ip = WifiUtils.getDeviceIpAddress()

        val address: String = String.format(
            this.getString(me.pengtao.filetransfer.R.string.http_address),
            ip,
            Constants.HTTP_PORT
        )
        binding.tvShowftp.text = address
        binding.tvShowftp.setOnClickListener {
            val cm = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("Label", address)
            cm.setPrimaryClip(mClipData)
            Toast.makeText(
                this,
                this.getString(me.pengtao.filetransfer.R.string.copy_toast),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}