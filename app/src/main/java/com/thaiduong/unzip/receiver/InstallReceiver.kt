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

package com.thaiduong.unzip.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.thaiduong.unzip.App
import com.thaiduong.unzip.ui.activities.InstallActivity
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.utils.ACTION_INSTALL
import com.thaiduong.unzip.utils.EXTENSION_FILE
import com.thaiduong.unzip.utils.INSTALL_STATUS
import com.thaiduong.unzip.utils.PATH

private const val TAG = "AppInstaller"

class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mIntent = Intent(context, InstallActivity::class.java)
        mIntent.flags = FLAG_ACTIVITY_NEW_TASK

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val activityIntent =
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

                context.startActivity(activityIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    .startTone(ToneGenerator.TONE_PROP_ACK)
                App.dataStore.putInt(INSTALL_STATUS, 1)
                mIntent.putExtra(ACTION_INSTALL, true)
                context.startActivity(mIntent)
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(TAG, "received $status and $msg")
                mIntent.putExtra(ACTION_INSTALL, false)
                context.startActivity(mIntent)
            }
        }
    }
}