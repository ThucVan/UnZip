package com.thaiduong.unzip.ui.bases.DropBox
import com.dropbox.core.android.Auth

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.dropbox.core.examples.android.internal.di.AppGraph
import com.dropbox.core.examples.android.internal.di.AppGraphImpl

abstract class BaseDropbox : AppCompatActivity() {

    private val appGraph: AppGraph = AppGraphImpl(this)

    protected val dropboxOAuthUtil get() = appGraph.dropboxOAuthUtil

    private val dropboxCredentialUtil get() = appGraph.dropboxCredentialUtil

    val dropboxApiWrapper get() = appGraph.dropboxApiWrapper

    // will use our Short Lived Token.
    override fun onResume() {
        super.onResume()
        dropboxOAuthUtil.onResume()
        if (isAuthenticated()) {
            loadData()
        }

        val prefs = getSharedPreferences("dropbox-sample", Context.MODE_PRIVATE)

        val uid = Auth.getUid()
        val storedUid = prefs.getString("user-id", null)
        if (uid != null && uid != storedUid) {
            prefs.edit().apply {
                putString("user-id", uid)
            }.apply()
        }
    }

    protected abstract fun loadData()

    protected fun isAuthenticated(): Boolean {
        return dropboxCredentialUtil.isAuthenticated()
    }
}