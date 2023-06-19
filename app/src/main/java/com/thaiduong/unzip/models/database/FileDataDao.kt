package com.thaiduong.unzip.models.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDataDelete(fileDataDelete: FileDataDelete)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDataSearch(fileDataSearch: FileDataSearch)

    @Query("select * from file_data_delete")
    fun getListDelete(): MutableList<FileDataDelete>?

    @Query("select * from file_data_search")
    fun getListSearch(): MutableList<FileDataSearch>?

    @Query("select originalPath from file_data_delete where fileName = :fileName")
    fun getOriginalPath(fileName : String) : String

    @Query("select count(*)!=0 from file_data_delete where fileName = :fileName and originalPath = :originalPath")
    fun isExitsDeleteFile(fileName: String, originalPath: String): Boolean

    @Query("select count(*)!=0 from file_data_search where fileName = :fileName and filePath = :filePath")
    fun isExistsSearchFile(fileName : String, filePath: String) : Boolean

    @Query("delete from file_data_delete WHERE fileName = :fileName and originalPath = :originalPath")
    fun deleteFileDelete(fileName: String, originalPath: String)

    @Query("delete from file_data_search WHERE fileName = :fileName and filePath = :filePath")
    fun deleteFileSearch(fileName : String, filePath: String)

    @Query("delete from file_data_delete")
    fun cleanRecycleBin()

}