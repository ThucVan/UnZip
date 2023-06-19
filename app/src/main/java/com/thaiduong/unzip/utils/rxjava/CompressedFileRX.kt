package com.thaiduong.unzip.utils.rxjava

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.thaiduong.unzip.R
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.*

class CompressedFileRX(
    private var activity: Activity,
    private var listFile: MutableList<FolderOrFile>,
    private var fileName: String,
    private var password: String,
    private var typeFile: String,
) {
    private lateinit var mDisposable: Disposable
    private var isSuccess = false

    fun letSubscribe(): Boolean {
        val observable = getObservable()
        val observer = getObserver()
        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
        return isSuccess
    }

    private fun getObserver(): Observer<Any> {
        return object : Observer<Any> {
            @SuppressLint("LogNotTimber")
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
        compressFile(activity, listFile, fileName, password, typeFile)
        return Observable.create { emitter ->
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }

    private fun compressFile(
        activity: Activity,
        listFile: MutableList<FolderOrFile>,
        fileName: String,
        password: String,
        typeFile: String,
    ): Boolean {
        val filesAndFolders =
            File("${activity.myGetExternalStorageDir()}/.Compressed").listFiles()!!
        for (mFile in filesAndFolders) {
            if (mFile.nameWithoutExtension == fileName) {
                Toast.makeText(
                    activity,
                    "Name already exists, please use another name",
                    Toast.LENGTH_LONG
                ).show()
                isSuccess = false
            }
        }

        try {
            val zipParameters = ZipParameters()
            zipParameters.isEncryptFiles = true
            zipParameters.encryptionMethod = EncryptionMethod.ZIP_STANDARD
            zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            var isDirectory: Boolean? = null

            val filesToAdd = mutableListOf<File>()
            for (file in listFile) {
                file.mFile?.let { filesToAdd.add(it) }
                isDirectory = file.mFile?.isDirectory
            }
            val compressedFileName = "${activity.myGetExternalStorageDir()}/.Compressed/$fileName.$typeFile"

            if (password.isBlank()) {
                val zipFile = ZipFile(compressedFileName)
                zipFile.isRunInThread = true
                if (isDirectory == true)
                    zipFile.addFolder(filesToAdd[0])
                else zipFile.addFiles(filesToAdd)
            } else {
                val zipFile =
                    ZipFile(compressedFileName, password.toCharArray())
                zipFile.isRunInThread = true
                zipFile.addFiles(filesToAdd, zipParameters)
            }
            dialogLoad(activity, "Compress Successfully !!!", "OK")
            isSuccess = true

        } catch (e: ZipException) {
            dialogLoad(activity, "Compress fails !!!", "OK")
            e.printStackTrace()
            Log.e("CompressedFileRX", "Compress fails: ${e.message}")
            isSuccess = false
        }
        return isSuccess
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun dialogLoad(context: Context, textTitle: String, textButton: String) {
        val dialog = Dialog(context, R.style.DialogStyle)
        dialog.setContentView(
            LayoutInflater.from(context).inflate(R.layout.compressing_dialog, null, false)
        )
        dialog.setCancelable(false)

        dialog.findViewById<ProgressBar>(R.id.progressBar).max = 10
        val currentProgress = 10
        ObjectAnimator.ofInt(
            dialog.findViewById<ProgressBar>(R.id.progressBar),
            "progress",
            currentProgress
        )
            .setDuration(2000)
            .start()
        Thread.sleep(100)

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnBackground).setOnClickListener {
            dialog.dismiss()
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            Log.e("CompressedFileRx", "loading : $e")
        }

        dialog.findViewById<Button>(R.id.btnBackground).visibility = View.GONE
        dialog.findViewById<TextView>(R.id.tv_compressing).text = textTitle
        dialog.findViewById<TextView>(R.id.tv_percent).text = "99%"
        dialog.findViewById<Button>(R.id.btnCancel).text = textButton
    }
}