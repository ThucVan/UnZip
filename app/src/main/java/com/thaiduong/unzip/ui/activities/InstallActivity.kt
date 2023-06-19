package com.thaiduong.unzip.ui.activities

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityInstallBinding
import com.thaiduong.unzip.ui.bases.BaseActivity
import com.thaiduong.unzip.utils.ACTION_INSTALL

class InstallActivity(override val layoutId: Int = R.layout.activity_install) :
    BaseActivity<ActivityInstallBinding>() {

    override fun initUi() {
        val isInstall = intent.getBooleanExtra(ACTION_INSTALL, false)
        if (isInstall) {

            binding.progressExtracted.setOnProgressListener {
                binding.relativeProgressBar.visibility = View.GONE
                binding.linearInstallResult.visibility = View.VISIBLE
            }

            var progress = 0
            val handler = @SuppressLint("HandlerLeak")
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == 0) {
                        if (progress < 100) {
                            progress++
                            binding.progressExtracted.progress = progress
                        }
                    }
                }
            }
            Thread {
                for (i in 0 until 100) {
                    try {
                        Thread.sleep(100)
                        handler.sendEmptyMessage(0)
                    } catch (e: InterruptedException) {
                        Log.e("ExtractFragment", "run failed: ${e.message}")
                    }
                }
            }.start()
        }
        else finish()
    }

    override fun doWork() {
        binding.btnOk.setOnClickListener {
            finish()
        }
    }
}