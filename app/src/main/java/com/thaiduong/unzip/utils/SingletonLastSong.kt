package com.thaiduong.unzip.utils

import android.content.Context
import android.content.SharedPreferences

class SingletonLastSong {
    companion object {
        private val sharePref = SingletonLastSong()
        private lateinit var sharedPreferences: SharedPreferences

        fun getInstance(context: Context): SingletonLastSong {
            if (!::sharedPreferences.isInitialized) {
                synchronized(SingletonLastSong::class.java) {
                    if (!::sharedPreferences.isInitialized) {
                        sharedPreferences = context.getSharedPreferences(
                            SHARED_PREFERENCES_LAST_SOUND,
                            Context.MODE_PRIVATE
                        )
                    }
                }
            }
            return sharePref
        }
    }

    var path: String?
        get() {
            return sharedPreferences.getString(SONG_NAME, "").toString()
        }
        set(path) {
            sharedPreferences.edit().putString(SONG_NAME, path).apply()
        }

}