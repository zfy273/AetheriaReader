package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "format")
    val format: String, // TXT, EPUB, PDF, RAR
    
    @ColumnInfo(name = "cover_path")
    val coverPath: String?,
    
    @ColumnInfo(name = "total_chapters")
    val totalChapters: Int = 0,
    
    @ColumnInfo(name = "last_read_chapter_id")
    val lastReadChapterId: Long? = null,
    
    @ColumnInfo(name = "last_read_position")
    val lastReadPosition: Long = 0L, // Byte offset or page index/scroll distance
    
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "progress_percentage")
    val progressPercentage: Float = 0f
)
