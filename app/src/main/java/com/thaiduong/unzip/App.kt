package com.thaiduong.unzip

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.tencent.mmkv.MMKV
import com.thaiduong.unzip.models.database.FileDatabase

class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var currentActivity: Activity? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContent: Context
        lateinit var dataStore: MMKV
        private lateinit var db: FileDatabase
        fun getDB() = db
    }

    override fun onCreate() {
        super.onCreate()
        appContent = applicationContext
        MMKV.initialize(this)
        dataStore = MMKV.defaultMMKV()
        db = Room.databaseBuilder(applicationContext, FileDatabase::class.java, "data")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        FirebaseApp.initializeApp(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        Log.e("App", "onActivityPaused: $activity")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.e("App", "onActivityStopped: $activity")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }
}