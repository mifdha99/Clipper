package com.example.data.model

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val durationSeconds: Int,
    val viewsCount: String,
    val thumbnailUrl: String,
    val videoStreamUrl: String,
    val description: String,
    val category: String
)
