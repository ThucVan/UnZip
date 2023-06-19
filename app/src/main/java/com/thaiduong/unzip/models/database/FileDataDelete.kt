package com.thaiduong.unzip.models.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_data_delete")
class FileDataDelete {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var fileName: String = ""
    var originalPath: String = ""
}
