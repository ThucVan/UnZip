package com.thaiduong.unzip.ui.bases

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewbinding.ViewBinding
import com.thaiduong.unzip.App
import java.util.*

abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: V
    protected abstract val layoutId: Int
    protected abstract fun initUi()
    protected abstract fun doWork()

    @SuppressLint("LogNotTimber")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setLanguages()
            binding = DataBindingUtil.setContentView(this, layoutId)
            initUi()
            doWork()
        } catch (e: Exception) {
            Log.e("BaseActivity", "onCreate: $e")
        }
    }

    @SuppressLint("LogNotTimber")
    private fun setLanguages() {
        if (App.dataStore.getString("lang", "").toString().isEmpty()) {
            if (Locale.getDefault().displayLanguage != "en")
                App.dataStore.putString("lang", "vi")
            else
                App.dataStore.putString("lang", "en")
        }
        val lang = App.dataStore.getString("lang", "")
        val config = resources.configuration
        if (lang != null && lang.isNotEmpty()) {
            Log.e("lange", lang.toString())
            val locale = Locale(lang)
            Locale.setDefault(locale)
            config.locale = locale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                createConfigurationContext(config)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }


}