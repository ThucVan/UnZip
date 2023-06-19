package com.thaiduong.unzip.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dropbox.core.examples.android.internal.api.DownloadFileTaskResult
import com.dropbox.core.examples.android.internal.api.DropboxUploadApiResponse
import com.dropbox.core.examples.android.internal.api.GetCurrentAccountResult
import com.dropbox.core.examples.android.internal.api.ListFolderApiResult
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thaiduong.unzip.App
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityDropboxBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.bases.DropBox.BaseDropbox
import com.thaiduong.unzip.ui.bases.DropBox.internal.ui.FilesAdapter
import com.thaiduong.unzip.utils.dialogLoader
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class DropboxActivity : BaseDropbox() {
    private var mPath: String? = null
    private var mFilesAdapter: FilesAdapter? = null
    private var mSelectedFile: FileMetadata? = null
    private var selectedFileList = ""
    private lateinit var mDisposable: Disposable

    private lateinit var binding: ActivityDropboxBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDropboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayUploadfile()
        mPath = intent.getStringExtra(EXTRA_PATH) ?: ""
        //login
        App.dataStore.putBoolean("checkLogout", isAuthenticated())
        Thread {
            this.runOnUiThread {
                run {
                    if (!isAuthenticated()) {
                        dropboxOAuthUtil.startDropboxAuthorization(this)
                    }
                }
            }
        }.start()


        mSelectedFile = null

        binding.imvBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnLogout.setOnClickListener {
            val dialog = Dialog(this, R.style.DialogStyle)
            dialog.setContentView(
                LayoutInflater.from(this).inflate(R.layout.sing_out_dialog, null, false)
            )
            dialog.setCancelable(false)
            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }

            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dropboxOAuthUtil.revokeDropboxAuthorization(dropboxApiWrapper)
                App.dataStore.putBoolean("checkLogout", isAuthenticated())
                dialog.dismiss()
                finish()
            }
            dialog.show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAccountInfo() {
        lifecycleScope.launch {
            when (val accountResult = dropboxApiWrapper.getCurrentAccount()) {
                is GetCurrentAccountResult.Error -> {
                    Timber.tag(javaClass.name).e(accountResult.e, "Failed to get account details.")
                    Toast.makeText(
                        applicationContext,
                        "Error getting account info!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is GetCurrentAccountResult.Success -> {
                    val account = accountResult.account
                    binding.tvTitleFolder.text =
                        "${getString(R.string.dropbox)} : ${account.name.displayName}"
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.e("checkLog", "onPause: ${App.dataStore.getBoolean("checkLogout", false)}")
    }

    override fun onResume() {
        super.onResume()
        App.dataStore.putBoolean("checkLogout", isAuthenticated())
        Log.e("checkLog", "onResume: ${App.dataStore.getBoolean("checkLogout", false)}")
        if (App.dataStore.getBoolean("checkLogout", false)) {
            letSubscribe()
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        App.dataStore.putString("foldername", "")
        App.dataStore.putBoolean("checkLogout", isAuthenticated())
    }

    override fun onStop() {
        super.onStop()
        App.dataStore.putString("fileSelected", "")
    }

    override fun loadData() {
        fetchAccountInfo()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val action = FileAction.fromCode(requestCode)
        var granted = true
        for (i in grantResults.indices) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Timber.tag(TAG)
                    .w("User denied " + permissions[i] + " permission to perform file action: " + action)
                granted = false
                break
            }
        }
        if (granted) {
            performAction(action)
        } else {
            when (action) {
                FileAction.UPLOAD -> Toast.makeText(
                    this,
                    "Can't upload file: read access denied. " +
                            "Please grant storage permissions to use this functionality.",
                    Toast.LENGTH_LONG
                )
                    .show()
                FileAction.DOWNLOAD -> Toast.makeText(
                    this,
                    "Can't download file: write access denied. " +
                            "Please grant storage permissions to use this functionality.",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun performAction(action: FileAction) {
        when (action) {
            FileAction.DOWNLOAD -> if (mSelectedFile != null) {
                downloadFile(mSelectedFile!!)
            }
            else -> {}
        }
    }

    private fun downloadFile(file: FileMetadata) {
        dialogLoader.createDialog(this)

        lifecycleScope.launch {
            val downloadFileTaskResult = dropboxApiWrapper.download(
                this@DropboxActivity.applicationContext, file
            )
            when (downloadFileTaskResult) {
                is DownloadFileTaskResult.Error -> {
                    dialogLoader.dismiss()
                    Timber.tag(TAG).e(downloadFileTaskResult.e, "Failed to download file.")
                    Toast.makeText(
                        this@DropboxActivity,
                        "An error has occurred",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DownloadFileTaskResult.Success -> {
                    dialogLoader.dismiss()
                    viewFileInExternalApp(
                        applicationContext,
                        downloadFileTaskResult.result
                    )
                }
            }
        }
    }

    private fun viewFileInExternalApp(context: Context, result: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mime = MimeTypeMap.getSingleton()
        val ext = result.name.substring(result.name.indexOf(".") + 1)
        val type = mime.getMimeTypeFromExtension(ext)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider", result
        )

        intent.setDataAndType(uri, type)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Check for a handler first to avoid a crash
        val manager = packageManager
        val resolveInfo = manager.queryIntentActivities(intent, 0)
        if (resolveInfo.size > 0) {
            startActivity(intent)
        }
    }

    private fun performWithPermissions(action: FileAction) {
        if (hasPermissionsForAction(action)) {
            performAction(action)
            return
        }
        if (shouldDisplayRationaleForAction(action)) {
            AlertDialog.Builder(this)
                .setMessage("This app requires storage access to download and upload files.")
                .setPositiveButton("OK") { dialog, which -> requestPermissionsForAction(action) }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        } else {
            requestPermissionsForAction(action)
        }
    }

    private fun hasPermissionsForAction(action: FileAction): Boolean {
        for (permission in action.permissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private fun shouldDisplayRationaleForAction(action: FileAction): Boolean {
        for (permission in action.permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true
            }
        }
        return false
    }

    private fun requestPermissionsForAction(action: FileAction) {
        ActivityCompat.requestPermissions(
            this,
            action.permissions,
            action.getCode()
        )
    }

    private enum class FileAction(vararg permissions: String) {
        DOWNLOAD(Manifest.permission.WRITE_EXTERNAL_STORAGE), UPLOAD(Manifest.permission.READ_EXTERNAL_STORAGE);

        val permissions: Array<String> = permissions.toList().toTypedArray()

        fun getCode(): Int {
            return ordinal
        }

        companion object {
            private val values = values()
            fun fromCode(code: Int): FileAction {
                require(!(code < 0 || code >= values.size)) { "Invalid FileAction code: $code" }
                return values[code]
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayUploadfile() {
        selectedFileList = App.dataStore.getString("fileSelected", "").toString()
        if (selectedFileList.isNotBlank()) binding.layoutUpload.visibility = View.VISIBLE
        val typeToken = object : TypeToken<MutableList<FolderOrFile>>() {}.type
        val list = Gson().fromJson<MutableList<FolderOrFile>>(selectedFileList, typeToken)

        if (list == null) {
            binding.layoutUpload.visibility = View.GONE
        } else {
            binding.layoutUpload.visibility = View.VISIBLE
            binding.tvListupload.text =
                "${getString(R.string.file_selceted)} ${list.size} ${getString(R.string.file)} \n ${
                    getString(R.string.tv_fileselected)
                }"
            //uploadFile(list)
            binding.btnUpload.setOnClickListener {
                uploadFile(list)
            }
        }
    }

    companion object {
        private val TAG = DropboxActivity::class.java.name
        const val EXTRA_PATH = "FilesActivity_Path"

        fun getIntent(context: Context?, path: String?): Intent {
            val filesIntent = Intent(context, DropboxActivity::class.java)
            filesIntent.putExtra(EXTRA_PATH, path)
            return filesIntent
        }
    }

    private fun uploadFile(listFile: MutableList<FolderOrFile>) {
        lifecycleScope.launch {
            dialogLoader.createDialog(this@DropboxActivity)
            var response: DropboxUploadApiResponse? = null
            for (i in listFile.indices) {
                val uri = Uri.fromFile(listFile[i].mFile!!.absoluteFile)
                val inputStream = contentResolver?.openInputStream(uri)
                val name = uri.lastPathSegment
                response = dropboxApiWrapper.uploadFile(
                    name!!,
                    inputStream!!,
                    App.dataStore.getString("foldername", "").toString()
                )
            }
            when (response) {
                is DropboxUploadApiResponse.Failure -> {
                    dialogLoader.dismiss()
                    binding.layoutUpload.visibility = View.GONE
                    Toast.makeText(
                        applicationContext,
                        "Error uploading file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DropboxUploadApiResponse.Success -> {
                    mPath?.let { path ->
                        lifecycleScope.launch {
                            when (val apiResult = dropboxApiWrapper.listFolders(path)) {
                                is ListFolderApiResult.Error -> {
                                    Timber.tag(TAG).e(apiResult.e, "Failed to list folder.")
                                    Toast.makeText(
                                        this@DropboxActivity,
                                        "An error has occurred",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                is ListFolderApiResult.Success -> {
                                    try {
                                        mFilesAdapter!!.setFiles(apiResult.result.entries)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }
                    }
                    dialogLoader.dismiss()
                    binding.layoutUpload.visibility = View.GONE
                    Toast.makeText(
                        applicationContext,
                        "Uploaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    fun loadDataRcv() {
        mFilesAdapter = FilesAdapter(
            dbxClientV2 = dropboxApiWrapper.dropboxClient,
            mCallback = object : FilesAdapter.Callback {
                override fun onFolderClicked(folder: FolderMetadata?) {
                    requireNotNull(folder)
                    startActivity(getIntent(this@DropboxActivity, folder.pathLower))
                    App.dataStore.putString("foldername", folder.pathLower)
                }

                override fun onFileClicked(file: FileMetadata?) {
                    mSelectedFile = file
                    performWithPermissions(FileAction.DOWNLOAD)
                }
            },
            scope = lifecycleScope, this@DropboxActivity
        )
        binding.rcvDropbox.layoutManager = LinearLayoutManager(this)
        binding.rcvDropbox.adapter = mFilesAdapter


        mPath?.let { path ->
            dialogLoader.createDialog(this)

            lifecycleScope.launch {
                when (val apiResult = dropboxApiWrapper.listFolders(path)) {
                    is ListFolderApiResult.Error -> {
                        dialogLoader.dismiss()
                        Timber.tag(TAG).e(apiResult.e, "Failed to list folder.")
                        Toast.makeText(
                            this@DropboxActivity,
                            "An error has occurred",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    is ListFolderApiResult.Success -> {
                        try {
                            mFilesAdapter!!.setFiles(apiResult.result.entries)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        dialogLoader.dismiss()
                    }
                }
            }
        }

    }

    fun letSubscribe() {
        val observable = getObservable()
        val observer = getObserver()
        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    private fun getObserver(): Observer<Any> {
        return object : Observer<Any> {
            override fun onSubscribe(d: Disposable) {
                mDisposable = d
                Log.e("CompressedFileRX", "onSubscribe")
            }

            override fun onNext(t: Any) {
                Log.e("CompressedFileRX", "onNext: $t")
            }

            override fun onError(e: Throwable) {
                Log.e("CompressedFileRX", "onError: $e")
            }

            override fun onComplete() {
                Log.e("CompressedFileRX", "onComplete")
                mDisposable.dispose()
            }
        }
    }

    private fun getObservable(): Observable<Any> {
        loadDataRcv()
        return Observable.create { emitter ->
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.layoutUpload.visibility = View.GONE
    }
}