package com.thaiduong.unzip.models.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "file_data_search")
class FileDataSearch {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var fileName: String = ""
    var filePath: String = ""

    override fun toString(): String {
        return "${fileName}\n${filePath}"
    }
}