package com.thaiduong.unzip.utils

import android.content.Context
import android.widget.Toast
import java.io.File

class FunctionApp {

    fun renameFile(context: Context, newName: String, currentFile: File): Boolean {
        return if (currentFile.exists() && newName.isNotEmpty()) {
            val newFile = if (currentFile.isFile)
                File(currentFile.parentFile, newName + "." + currentFile.extension)
            else File(currentFile.parentFile, newName)
            if (currentFile.renameTo(newFile)) {
                Toast.makeText(context, "Rename Successfully !!", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(
                    context,
                    "folder name already exists, please choose another name",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        } else {
            Toast.makeText(context, "Please do not leave the name blank", Toast.LENGTH_SHORT).show()
            return false
        }
    }

}