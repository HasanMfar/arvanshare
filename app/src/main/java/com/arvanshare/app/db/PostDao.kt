package com.arvanshare.app.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Upsert
    suspend fun upsert(entity: PostEntity)
}
