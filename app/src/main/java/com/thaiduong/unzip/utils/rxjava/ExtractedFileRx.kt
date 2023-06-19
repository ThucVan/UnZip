package com.thaiduong.unzip.utils.rxjava

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.hzy.libp7zip.P7ZipApi
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.lingala.zip4j.ZipFile
import java.io.File

class ExtractedFileRx(
    private var activity: Activity,
    private var path: String,
    private var password: String,
    private var fileName: String
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
                Log.e("ExtractedFileRx", "onSubscribe")
            }

            @SuppressLint("LogNotTimber")
            override fun onNext(t: Any) {
                Log.e("ExtractedFileRx", "onNext: $t")
            }

            @SuppressLint("LogNotTimber")
            override fun onError(e: Throwable) {
                Log.e("ExtractedFileRx", "onError: $e")
            }

            @SuppressLint("LogNotTimber")
            override fun onComplete() {
                Log.e("ExtractedFileRx", "onComplete")
                mDisposable.dispose()
            }
        }
    }

    private fun getObservable(): Observable<Any> {
        extractedFile(activity, path, password, fileName)
        return Observable.create { emitter ->
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun extractedFile(
        activity: Activity,
        path: String,
        password: String,
        fileName: String
    ) {
        val filesAndFolders =
            File("${activity.myGetExternalStorageDir()}/.Extracted").listFiles()!!
        for (mFile in filesAndFolders) {
            if (mFile.nameWithoutExtension == fileName) {
                Toast.makeText(
                    activity,
                    "Name already exists, please use another name",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        val folder = File("${activity.myGetExternalStorageDir()}/.Extracted", fileName)
        if (!folder.exists()) folder.mkdir()
        try {
            when (File(path).extension) {
                "zip" -> {
                    ZipFile(path, password.toCharArray()).extractAll(folder.absolutePath)
                    isSuccess = true
                }
                "rar", "7z", "tar" -> {
                    try {
                        val cmd = String.format("7z x '%s' '-o%s' -aoa", path, folder.absolutePath)
                        P7ZipApi.executeCommand(cmd)
                        isSuccess = true
                    } catch (e: Exception) {
                        Log.e("ExtractedFileRx", e.message.toString())
                    }
                }
                else -> {
                    folder.delete()
                    Toast.makeText(
                        activity,
                        "This file cannot be unzipped",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (ex: Exception) {
            folder.delete()
            Toast.makeText(
                activity,
                "The password to extract the file is incorrect, please re-enter the password",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}