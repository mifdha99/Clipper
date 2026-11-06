package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_clips")
data class VideoClipEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sourceVideoTitle: String,
    val sourceVideoUrl: String,
    val sourceThumbnailUrl: String,
    val inTimestampMs: Long,
    val outTimestampMs: Long,
    val durationMs: Long,
    val aspectRatio: String, // "9:16", "1:1", "16:9"
    val overlayText: String,
    val overlayStyle: String, // "viral_bold", "minimal_white", "neon_glow", "subtitle_box"
    val transitionEffect: String, // "none", "fade", "zoom_punch", "glitch", "slide"
    val speedMultiplier: Float, // 0.5f, 1.0f, 1.5f, 2.0f
    val createdAt: Long,
    val tags: String
)
