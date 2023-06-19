package com.thaiduong.unzip.utils

import com.thaiduong.unzip.models.FolderOrFile
import java.io.File

class FileManager {
    companion object {
        fun getFile(fileList: MutableList<FolderOrFile>, typeList: List<String>, fileRoot: File) {
            if (fileRoot.name.first() == '.' && fileRoot.name != ".Extracted") return
            if (fileRoot.name == "Android") return
            if (fileRoot.isDirectory) {
                val filesAndFolders = fileRoot.listFiles()!!
                for (file in filesAndFolders) {
                    getFile(fileList, typeList, file)
                }
            } else {
                if (typeList.contains(fileRoot.extension) && !fileList.contains(FolderOrFile(fileRoot)))
                    fileList.add(FolderOrFile(fileRoot))
            }
        }
    }
}