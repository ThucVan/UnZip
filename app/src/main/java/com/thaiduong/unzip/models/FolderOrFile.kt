package com.thaiduong.unzip.models

import java.io.File

data class FolderOrFile(
    var mFile: File? = null,
    var isSelected: Boolean = false
)
