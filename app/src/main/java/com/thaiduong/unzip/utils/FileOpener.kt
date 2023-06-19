package com.thaiduong.unzip.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object FileOpener {
    @Throws(IOException::class)
    fun openFile(context: Context, file: File) {
        val fileUri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
        if (fileUri.toString().contains(".doc")) {
            intent.setDataAndType(fileUri, "application/msword")
        } else if (fileUri.toString().contains(".pdf")) {
            intent.setDataAndType(fileUri, "application/pdf")
        } else if (fileUri.toString().contains(".mp3") || fileUri.toString().contains(".wav")) {
            intent.setDataAndType(fileUri, "audio/x-wav")
        } else if (fileUri.toString().contains(".jpeg") || fileUri.toString()
                .contains(".jpg") || fileUri.toString().contains(".png")
        ) {
            intent.setDataAndType(fileUri, "image/jpeg")
        } else if (fileUri.toString().contains(".mp4")) {
            intent.setDataAndType(fileUri, "video/*")
        } else {
            intent.setDataAndType(fileUri, "*/*")
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    @Throws(IOException::class)
    fun openFileTwo(context: Context, fileUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        if (fileUri.toString().contains(".mp3") || fileUri.toString().contains(".wav")) {
            intent.setDataAndType(fileUri, "audio/x-wav")
        } else {
            intent.setDataAndType(fileUri, "*/*")
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}