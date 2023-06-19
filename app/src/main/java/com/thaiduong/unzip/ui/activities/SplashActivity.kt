package com.thaiduong.unzip.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.thaiduong.unzip.App
import com.thaiduong.unzip.databinding.ActivitySplashBinding
import com.thaiduong.unzip.utils.IS_INSTALL_FIRST_APP

@SuppressLint("CustomSplashScreen")
class SplashActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
        doWork()
    }

    fun initUi() {
        if (App.dataStore.getBoolean(IS_INSTALL_FIRST_APP, true)) {
            binding.linearOnBoarding.visibility = View.VISIBLE
        } else {
            binding.linearOnBoarding.visibility = View.GONE
            start()
        }
    }

    fun doWork() {
        binding.btnStart.setOnClickListener { start() }
    }

    private fun start() {
        App.dataStore.putBoolean(IS_INSTALL_FIRST_APP, false)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}