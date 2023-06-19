package com.thaiduong.unzip.ui.bases.DropBox

import android.app.Application
import com.dropbox.core.examples.android.internal.di.AppGraph
import com.dropbox.core.examples.android.internal.di.AppGraphImpl

class DropboxApplication: Application() {
    val appGraph: AppGraph = AppGraphImpl(this)
}