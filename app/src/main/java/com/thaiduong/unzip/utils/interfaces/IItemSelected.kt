package com.thaiduong.unzip.utils.interfaces

import com.thaiduong.unzip.models.FolderOrFile

interface IItemSelected {
    fun selectedItem(mFolderOrFile: FolderOrFile)
}