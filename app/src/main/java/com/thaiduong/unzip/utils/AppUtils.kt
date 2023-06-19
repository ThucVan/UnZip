package com.thaiduong.unzip.utils

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.thaiduong.unzip.App.Companion.getDB
import com.thaiduong.unzip.BuildConfig
import com.thaiduong.unzip.R
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.models.Song
import com.thaiduong.unzip.models.database.FileDataDelete
import com.thaiduong.unzip.utils.customclass.BottomSheetFunction
import com.thaiduong.unzip.utils.rxjava.CopyFileRx
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Method

object AppUtils {

    fun Activity.myGetExternalStorageDir(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val myStorageManager =
                this.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
            val mySV = myStorageManager.primaryStorageVolume
            return mySV.directory!!.path
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                var volumeRootPath = ""
                val myStorageManager =
                    this.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
                val mySV = myStorageManager.primaryStorageVolume
                val storageVolumeClazz: Class<*>?
                try {
                    storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
                    val getPath: Method = storageVolumeClazz.getMethod("getPath")
                    volumeRootPath = getPath.invoke(mySV) as String
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return volumeRootPath
            } else {
                return Environment.getExternalStorageDirectory().path
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun Activity.getFile(
        filesAndFolders: Array<File>?,
        arr: MutableList<FolderOrFile>,
        itemAmount: Int
    ) {
        if (filesAndFolders != null && arr.size <= itemAmount) {
            for (i in filesAndFolders.indices) {
                if (arr.size >= itemAmount) return
                if (filesAndFolders[i].isDirectory) { // if its a directory need to get the files under that directory
                    this.getFile(filesAndFolders[i].listFiles(), arr, itemAmount)
                } else { // add path of  files to your arraylist for later use
                    if (filesAndFolders[i].parentFile?.name != ".Recycle Bin")
                    //Do what ever u want
                        arr.add(FolderOrFile(filesAndFolders[i]))
                }
            }
        }
    }

    @SuppressLint("InflateParams", "NotifyDataSetChanged")
    fun Activity.moveRecycleBin(
        activity: Activity? = null,
        originalList: MutableList<FolderOrFile>? = null,
        selectedList: MutableList<FolderOrFile>,
        mBottomSheetFunction: BottomSheetFunction? = null
    ) {
        val dialog = Dialog(this, R.style.DialogStyle)
        dialog.setContentView(
            LayoutInflater.from(this).inflate(R.layout.move_recycle_bin_dialog, null, false)
        )
        dialog.setCancelable(false)

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        var isDelete = false
        dialog.findViewById<RelativeLayout>(R.id.relativeSelected).setOnClickListener {
            isDelete = if (!isDelete) {
                dialog.findViewById<ImageView>(R.id.imvSelected)
                    .setImageResource(R.drawable.ic_selected)
                true
            } else {
                dialog.findViewById<ImageView>(R.id.imvSelected)
                    .setImageResource(R.drawable.ic_unselected)
                false
            }
        }

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            val progressDialog = this.initProgressBarDialog("Move Recycle Bin")
            progressDialog.show()
            Thread {
                activity?.runOnUiThread {
                run {
                    for (file in selectedList) {
                        file.mFile?.absolutePath?.let { it1 -> this.deleteFile(it1, isDelete) }
                        originalList?.remove(file)
                    }
                    selectedList.clear()
                    if (originalList == null) activity?.finish()
                    if (originalList?.isEmpty() == true)
                        activity?.findViewById<ImageView>(R.id.imvNoFilesFound)?.visibility =
                            View.VISIBLE
                    mBottomSheetFunction?.turnOffBottomSheet()
                    progressDialog.dismiss()
                }
                }
            }.start()
        }
        dialog.show()
    }

    fun Activity.getSongFromPhone(folderName: String): ArrayList<Song> {
        val tempAudioList: ArrayList<Song> = ArrayList()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ALBUM,
            MediaStore.Audio.ArtistColumns.ARTIST,
            MediaStore.Audio.Media._ID
        )
        val c: Cursor? = contentResolver.query(
            uri,
            projection,
            MediaStore.Audio.Media.DATA + " like ? ",
            arrayOf("%$folderName%"),
            null
        )
        if (c != null) {
            while (c.moveToNext()) {
                val path: String? = c.getString(0) // Retrieve path.
                val name: String? = c.getString(1) // Retrieve name.
                val id: String? = c.getString(4)
                tempAudioList.add(Song(id, name, path))
            }
            c.close()
        }
        return tempAudioList
    }

    fun Activity.deleteFile(inputPath: String, isDeleteCompletely: Boolean) {
        val mFile = File(inputPath)
        if (isDeleteCompletely) {
            // delete the original file
            if (mFile.isDirectory) {
                mFile.deleteRecursively()
            } else {
                mFile.delete()
            }
        } else {
            if (mFile.isDirectory) {
                File("${this.myGetExternalStorageDir()}/.Recycle Bin", File(inputPath).name).mkdir()
            }
            val output = "${this.myGetExternalStorageDir()}/.Recycle Bin/${File(inputPath).name}"
            val pathMap = hashMapOf<String, String>()
            val fileDataDelete = FileDataDelete()
            fileDataDelete.originalPath = inputPath
            fileDataDelete.fileName = File(inputPath).name
            if (!getDB().fileDataDao().isExitsDeleteFile(File(inputPath).name, inputPath))
                getDB().fileDataDao().insertDataDelete(fileDataDelete)

            getFileListInFolder(mFile, output, pathMap)

            val mCopyFileRx = CopyFileRx(pathMap)
            mCopyFileRx.letSubscribe()
            if (mFile.isDirectory) {
                mFile.deleteRecursively()
            } else {
                mFile.delete()
            }
        }
    }

    private fun getFileListInFolder(file: File, output: String, pathMap: HashMap<String, String>) {
        if (file.isDirectory) {
            if (file.listFiles()?.isEmpty() == true) {
                pathMap[file.absolutePath] = output
            } else {
                for (f in file.listFiles()!!) {
                    if (f.isDirectory) {
                        File(output, f.name).mkdir()
                        getFileListInFolder(f, "$output/${f.name}", pathMap)
                    } else {
                        pathMap[f.absolutePath] = "$output/${f.name}"
                    }
                }
            }
        } else {
            pathMap[file.absolutePath] = output
        }
    }

    @SuppressLint("LogNotTimber")
    fun restoreFile(inputPath: String, outputPath: String) {
        try {
            val mFile = File(inputPath)
            getDB().fileDataDao().deleteFileDelete(File(inputPath).name, inputPath)
            val pathMap = hashMapOf<String, String>()
            if (mFile.isDirectory) {
                File(outputPath).mkdir()
            }
            getFileListInFolder(mFile, outputPath, pathMap)
            val mCopyFileRx = CopyFileRx(pathMap)
            mCopyFileRx.letSubscribe()
            if (mFile.isDirectory) {
                mFile.deleteRecursively()
            } else {
                // delete the original file
                File(inputPath).delete()
            }
        } catch (ex: FileNotFoundException) {
            Log.e("AppUtility", "FileNotFoundException: ${ex.message.toString()}")
        } catch (ex: IOException) {
            Log.e("AppUtility", "IOException: ${ex.message.toString()}")
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    fun Activity.initProgressBarDialog(title: String): Dialog {
        val dialog = Dialog(this, R.style.DialogStyle)
        dialog.setContentView(
            LayoutInflater.from(this).inflate(R.layout.progress_bar_dialog, null, false)
        )
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.tv_title).text = title
        dialog.findViewById<ProgressBar>(R.id.progressBar).max = 10
        val currentProgress = 9
        ObjectAnimator.ofInt(
            dialog.findViewById<ProgressBar>(R.id.progressBar),
            "progress",
            currentProgress
        )
            .setDuration(2000)
            .start()
        Thread.sleep(100)

        dialog.findViewById<TextView>(R.id.tv_percent).text = "99%"
        return dialog
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun Activity.contact() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(MY_EMAIL))
        intent.data = Uri.parse("mailto:")
        if (intent.resolveActivity(this.packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "There is no application that support this action",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun Activity.reviewApp() {
        val applicationNameId = BuildConfig.APPLICATION_ID
        val uri = Uri.parse("market://details?id=$applicationNameId")
        val gotoMarket = Intent(Intent.ACTION_VIEW, uri)
        gotoMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            startActivity(gotoMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$applicationNameId")
                )
            )
        }
    }

    @SuppressLint("LogNotTimber")
    fun Activity.shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareContent =
                "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, SUBJECT)
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent)
            startActivity(Intent.createChooser(shareIntent, "Go to link: "))
        } catch (e: Exception) {
            Log.e("AppUtility", "share failed")
        }
    }

    fun shareFile(context: Context, pathList: MutableList<String>, type: String) {
        try {
            val intent = Intent()
            intent.type = "$type/*"
            val uris: ArrayList<Uri> = ArrayList()
            for (path in pathList) {
                val uri: Uri =
                    FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        File(path)
                    )
                uris.add(uri)
            }
            if (uris.size == 1) {
                intent.setDataAndType(uris[0], context.contentResolver.getType(uris[0]))
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                intent.action = Intent.ACTION_SEND_MULTIPLE
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, "Share File"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}