package com.thaiduong.unzip.utils.customclass

import android.app.Activity
import android.util.Log
import com.thaiduong.unzip.utils.interfaces.IGetIListFilePath
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class GetListFilePath(private var mActivity: Activity, private var filesAndFolders: Array<File>) {
    private lateinit var mIGetIListFilePath: IGetIListFilePath
    private lateinit var mDisposable: Disposable
    private lateinit var mFilePathList: ArrayList<String>
    private var pathArr = arrayListOf<String>()

    fun letSubscribe() {
        mIGetIListFilePath = mActivity as IGetIListFilePath
        val observablePath = getObservablePath()
        val observerPath = getObserverPath()
        observablePath.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observerPath)
    }

    private fun getObserverPath(): Observer<String> {
        return object : Observer<String> {
            override fun onSubscribe(d: Disposable) {
                mDisposable = d
                mFilePathList = arrayListOf()
            }

            override fun onNext(t: String) {
                mFilePathList.add(t)
            }

            override fun onError(e: Throwable) {
                Log.e("GetListFilePath", "onError: $e")
            }

            override fun onComplete() {
                mIGetIListFilePath.getFilePathList(mFilePathList)
                mDisposable.dispose()
            }
        }
    }

    private fun getObservablePath(): Observable<String> {
        getAllFileAndFolder(filesAndFolders)
        return Observable.create { emitter ->
            if (pathArr.isEmpty()) {
                emitter.onError(Exception())
            }
            for (value in pathArr) {
                if (!emitter.isDisposed) {
                    emitter.onNext(value)
                }
            }
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }

    private fun getAllFileAndFolder(filesAndFolders: Array<File>?) {
        if (filesAndFolders != null) {
            for (i in filesAndFolders.indices) {
                if (filesAndFolders[i].name in arrayOf(".Recycle Bin", "Android")) continue
                if (filesAndFolders[i].isDirectory) { // if its a directory need to get the files under that directory
                    pathArr.add(filesAndFolders[i].absolutePath)
                    getAllFileAndFolder(filesAndFolders[i].listFiles())
                } else { // add path of  files to your arraylist for later use

                    //Do what ever u want
                    pathArr.add(filesAndFolders[i].absolutePath)
                }
            }
        }
    }
}