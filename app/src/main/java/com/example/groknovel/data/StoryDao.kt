package com.example.groknovel.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 故事数据访问对象 (DAO)
 * 提供对 stories 表的 CRUD 操作
 */
@Dao
interface StoryDao {

    /**
     * 插入新故事
     * @param story 故事实体
     * @return 插入的行 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(story: StoryEntity): Long

    /**
     * 获取所有故事，按创建时间降序排列
     * @return Flow<List<StoryEntity>> 故事的实时数据流
     */
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    /**
     * 获取单个故事
     * @param id 故事 ID
     * @return StoryEntity?
     */
    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Int): StoryEntity?

    /**
     * 删除指定故事
     * @param story 故事实体
     */
    @Delete
    suspend fun delete(story: StoryEntity)

    /**
     * 删除所有故事
     */
    @Query("DELETE FROM stories")
    suspend fun deleteAll()

    /**
     * 获取故事数量
     * @return 故事总数
     */
    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int
}
