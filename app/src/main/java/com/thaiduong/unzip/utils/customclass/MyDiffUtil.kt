package com.thaiduong.unzip.utils.customclass

import androidx.recyclerview.widget.DiffUtil
import com.thaiduong.unzip.models.FolderOrFile

class MyDiffUtil(
    private val oldArr: MutableList<FolderOrFile>,
    private val newArr: MutableList<FolderOrFile>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldArr.size
    }

    override fun getNewListSize(): Int {
        return newArr.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArr[oldItemPosition].mFile == newArr[newItemPosition].mFile &&
                oldArr[oldItemPosition].isSelected == newArr[newItemPosition].isSelected
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArr[oldItemPosition].mFile == newArr[newItemPosition].mFile &&
                oldArr[oldItemPosition].isSelected == newArr[newItemPosition].isSelected
    }

}