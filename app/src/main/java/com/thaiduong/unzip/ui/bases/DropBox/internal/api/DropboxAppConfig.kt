package com.dropbox.core.examples.android.internal.api

import com.thaiduong.unzip.BuildConfig

class DropboxAppConfig(
    val apiKey : String = BuildConfig.DROPBOX_APP_KEY,
    val clientIdentifier: String = "db-${apiKey}"
)