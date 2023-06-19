package com.thaiduong.unzip.utils.interfaces

import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.adapters.CategoryAdapter

interface IGetItemCategory {
    fun sentData(list: MutableList<FolderOrFile>, categoryAdapter: CategoryAdapter)
}