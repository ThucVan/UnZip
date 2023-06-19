/*
  Copyright (c) 2019 CommonsWare, LLC

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain	a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.

  Covered in detail in the book _Elements of Android Q

  https://commonsware.com/AndroidQ
*/

package com.thaiduong.unzip.utils.customview

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thaiduong.unzip.receiver.InstallReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NAME = "mostly-unused"
private const val PI_INSTALL = 3439

class InstallApk(app: Application) : AndroidViewModel(app) {
    private val installer = app.packageManager.packageInstaller
    private val resolver = app.contentResolver

    fun install(apkUri: Uri) {
        viewModelScope.launch(Dispatchers.Main) {
            installCoroutine(apkUri)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private suspend fun installCoroutine(apkUri: Uri) =
        withContext(Dispatchers.IO) {
            resolver.openInputStream(apkUri)?.use { apkStream ->
                val length =
                    DocumentFile.fromSingleUri(getApplication(), apkUri)?.length() ?: -1
                val params =
                    PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = installer.createSession(params)
                val session = installer.openSession(sessionId)

                session.openWrite(NAME, 0, length).use { sessionStream ->
                    apkStream.copyTo(sessionStream)
                    session.fsync(sessionStream)
                }

                val intent = Intent(getApplication(), InstallReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    getApplication(),
                    PI_INSTALL,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                session.commit(pi.intentSender)
                session.close()
            }
        }
}