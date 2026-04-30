package com.example.groknovel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 故事实体类，对应 Room 数据库中的 stories 表
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val genre: String,
    val protagonist: String,
    val createdAt: Long = System.currentTimeMillis()
)
