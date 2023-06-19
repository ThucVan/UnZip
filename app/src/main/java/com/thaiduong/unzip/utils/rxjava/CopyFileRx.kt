package com.thaiduong.unzip.utils.rxjava

import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.*

class CopyFileRx(private var pathMap: HashMap<String, String>) {
    private lateinit var mDisposable: Disposable

    fun letSubscribe(): Boolean {
        val observable = getObservable()
        val observer = getObserver()
        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
        return true
    }

    private fun getObserver(): Observer<Any> {
        return object : Observer<Any> {
            override fun onSubscribe(d: Disposable) {
                mDisposable = d
                Log.e("CopyFileRx", "onSubscribe")
            }

            override fun onNext(t: Any) {
                Log.e("CopyFileRx", "onNext: $t")
            }

            override fun onError(e: Throwable) {
                Log.e("CopyFileRx", "onError: $e")
            }

            override fun onComplete() {
                Log.e("CopyFileRx", "onComplete")
                mDisposable.dispose()
            }
        }
    }

    private fun getObservable(): Observable<Any> {
        copyFile(pathMap)
        return Observable.create { emitter ->
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }

    private fun copyFile(pathMap: HashMap<String, String>) {
        try {
            for ((inputPath, outputPath) in pathMap) {
                val `in`: InputStream = FileInputStream(inputPath)
                val out: OutputStream = FileOutputStream(outputPath)

                val buffer = ByteArray(1024)
                var read: Int
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()

                // write the output file
                out.flush()
                out.close()
            }

        } catch (ex: FileNotFoundException) {
            Log.e("CopyFileRx", "FileNotFoundException: ${ex.message.toString()}")
        } catch (ex: IOException) {
            Log.e("CopyFileRx", "IOException: ${ex.message.toString()}")
        }
    }

}