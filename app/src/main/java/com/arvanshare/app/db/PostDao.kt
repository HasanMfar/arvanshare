package com.arvanshare.app.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT postId FROM posts")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM posts WHERE postId = :postId")
    suspend fun deleteById(postId: String)

    @Upsert
    suspend fun upsert(entity: PostEntity)
}
