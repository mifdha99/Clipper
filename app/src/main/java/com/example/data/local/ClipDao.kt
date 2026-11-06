package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VideoClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM video_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<VideoClipEntity>>

    @Query("SELECT * FROM video_clips WHERE id = :clipId LIMIT 1")
    suspend fun getClipById(clipId: String): VideoClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: VideoClipEntity)

    @Delete
    suspend fun deleteClip(clip: VideoClipEntity)

    @Query("DELETE FROM video_clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: String)
}
