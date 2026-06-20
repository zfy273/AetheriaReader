package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"])
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "chapter_index")
    val chapterIndex: Int,
    
    @ColumnInfo(name = "chapter_title")
    val chapterTitle: String,
    
    // Critical for TXT byte-offset loading
    @ColumnInfo(name = "start_byte")
    val startByte: Long = 0L,
    
    @ColumnInfo(name = "end_byte")
    val endByte: Long = 0L
)
