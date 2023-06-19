package com.thaiduong.unzip.models.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FileDataDelete::class, FileDataSearch::class], version = 1, exportSchema = false)
abstract class FileDatabase : RoomDatabase() {
    abstract fun fileDataDao() : FileDataDao
}
