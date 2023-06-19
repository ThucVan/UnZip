package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.thaiduong.unzip.App
import com.thaiduong.unzip.R
import com.thaiduong.unzip.utils.IS_SHOW_FILE_RECENTLY
import com.thaiduong.unzip.utils.interfaces.IShowRecentlyFile
import java.util.*
import kotlin.collections.HashMap

class MenuAdapter internal constructor(
    private val context: Context,
    private val titleList: List<String>,
    private val dataList: HashMap<String, List<String>>,
) : BaseExpandableListAdapter() {
    private var mIShowRecentlyFile = context as IShowRecentlyFile

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.titleList[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    @SuppressLint("InflateParams", "CutPasteId")
    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var mConvertView = convertView
        if (mConvertView == null) {
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            mConvertView = layoutInflater.inflate(R.layout.menu_child, null)
        }
        mConvertView!!.findViewById<TextView>(R.id.tvMenuTitle).text =
            getChild(listPosition, expandedListPosition) as String
        when (App.dataStore.getString("lang", "")) {
            "en" -> mConvertView.findViewById<TextView>(R.id.tvLanguage).setText(R.string.Eng)
            "vi" -> mConvertView.findViewById<TextView>(R.id.tvLanguage).setText(R.string.vn)
        }
        when (expandedListPosition) {
            0 -> {
                mConvertView.findViewById<SwitchCompat>(R.id.btnOnOff).isChecked =
                    App.dataStore.getBoolean(IS_SHOW_FILE_RECENTLY, true)
                mConvertView.findViewById<TextView>(R.id.tvLanguage).visibility = View.GONE
            }
            1 -> {
                mConvertView.findViewById<SwitchCompat>(R.id.btnOnOff).visibility = View.GONE
            }
            else -> {
                mConvertView.findViewById<SwitchCompat>(R.id.btnOnOff).visibility = View.GONE
                mConvertView.findViewById<TextView>(R.id.tvLanguage).visibility = View.GONE
            }
        }

        mConvertView.findViewById<SwitchCompat>(R.id.btnOnOff)
            .setOnCheckedChangeListener { _, isChecked ->
                App.dataStore.putBoolean(IS_SHOW_FILE_RECENTLY, isChecked)
                mIShowRecentlyFile.isShowFile(isChecked)
            }
        return mConvertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.titleList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.titleList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.titleList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    @SuppressLint("InflateParams")
    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var mConvertView = convertView
        if (mConvertView == null) {
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            mConvertView = layoutInflater.inflate(R.layout.menu_group, null)
        }
        mConvertView!!.findViewById<TextView>(R.id.tvSetting).text =
            getGroup(listPosition) as String
        when (listPosition) {
            0 -> {
                mConvertView.findViewById<ImageView>(R.id.imvSetting)
                    .setImageResource(R.drawable.ic_recycle_bin)
                mConvertView.findViewById<ImageView>(R.id.imvExpanded).visibility = View.INVISIBLE
            }
            1 -> {
                mConvertView.findViewById<ImageView>(R.id.imvSetting)
                    .setImageResource(R.drawable.ic_setting)
                mConvertView.findViewById<ImageView>(R.id.imvExpanded).visibility = View.VISIBLE
                mConvertView.findViewById<ImageView>(R.id.imvDivider).visibility = View.GONE
            }
        }

        return mConvertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

}