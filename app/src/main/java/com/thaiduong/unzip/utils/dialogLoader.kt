package com.thaiduong.unzip.utils

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.style.Circle
import com.thaiduong.unzip.R

object dialogLoader {
    private var processingDialog: AlertDialog? = null
    fun createDialog(activity: Activity) {
        try {
            val layout =
                LayoutInflater.from(activity).inflate(R.layout.custom_dialog_progress, null)
            processingDialog = AlertDialog.Builder(activity).create()
            processingDialog?.setCanceledOnTouchOutside(false)
            processingDialog?.setView(layout)
            processingDialog?.show()
            processingDialog?.setOnKeyListener { arg0, keyCode, event ->
                activity.finish()
                true
            }
            processingDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val progressBarLoading = processingDialog?.findViewById<SpinKitView>(R.id.spin_kit)
            progressBarLoading?.setIndeterminateDrawable(Circle())
        } catch (e: Exception) {
            Log.e("TAG", "createDialog: $e")
        }
    }

    fun dismiss() {
        processingDialog?.dismiss()
        processingDialog = null
    }
}