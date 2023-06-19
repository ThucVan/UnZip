package com.thaiduong.unzip.models

import android.content.Context
import android.content.res.Resources
import android.provider.Settings.Global.getString
import com.thaiduong.unzip.App
import com.thaiduong.unzip.R

object SettingModel {

    fun getData(context:Context): LinkedHashMap<String, List<String>> {
        val menuTitle = LinkedHashMap<String, List<String>>()
        val setting = mutableListOf<String>()
        val recycle = mutableListOf<String>()

        setting.add(context.getString(R.string.recently))
        setting.add(context.getString(R.string.language))
        setting.add(context.getString(R.string.privavy_policy))
        setting.add(context.getString(R.string.term))
        setting.add(context.getString(R.string.contact))
        setting.add(context.getString(R.string.shareApp))

        menuTitle[context.getString(R.string.recycle_bin)] = recycle
        menuTitle[context.getString(R.string.setting)] = setting

        return menuTitle
    }

}