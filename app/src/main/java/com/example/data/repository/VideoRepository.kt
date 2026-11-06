package com.example.data.repository

import com.example.data.model.YouTubeVideo
import java.util.regex.Pattern

class VideoRepository {

    // Reliable public stream URLs for smooth Android video playback and testing
    private val sampleVideos = listOf(
        YouTubeVideo(
            id = "dQw4w9WgXcQ",
            title = "Top 10 Tech Innovations in 2026: AI Revolution",
            channel = "TechPulse Media",
            durationSeconds = 180,
            viewsCount = "1.8M views",
            thumbnailUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80",
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "A deep dive into futuristic AI hardware, personal robotics, and mobile neural engines.",
            category = "Technology"
        ),
        YouTubeVideo(
            id = "jNQXAC9IVRw",
            title = "Exclusive Podcast: How Creators Make Viral Reels",
            channel = "Creator Studio Talk",
            durationSeconds = 240,
            viewsCount = "920K views",
            thumbnailUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=800&q=80",
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            description = "Inside secrets on hook timing, audio editing, and the 3-second retention rule for TikTok.",
            category = "Podcast"
        ),
        YouTubeVideo(
            id = "fJ9rUzIMcZQ",
            title = "Pro Gaming Championship: Insane 1v4 Clutch Moment",
            channel = "Apex Arena Masters",
            durationSeconds = 120,
            viewsCount = "3.4M views",
            thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&q=80",
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            description = "High octane tournament final with mind-blowing reflexes and screaming crowd commentary.",
            category = "Gaming"
        ),
        YouTubeVideo(
            id = "kJQP7kiw5Fk",
            title = "Hidden Street Food Gems in Tokyo Night Market",
            channel = "Wanderlust Eats",
            durationSeconds = 210,
            viewsCount = "2.1M views",
            thumbnailUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80",
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            description = "Crispy wagyu skewers, steaming ramen bowls, and secret alleys of Shinjuku.",
            category = "Vlog & Food"
        ),
        YouTubeVideo(
            id = "9bZkp7q19f0",
            title = "5 Science Experiments That Look Like Magic",
            channel = "Crazy Science Lab",
            durationSeconds = 150,
            viewsCount = "5.7M views",
            thumbnailUrl = "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800&q=80",
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            description = "Mind-boggling chemistry and physics reactions with explosive visual effects.",
            category = "Science"
        )
    )

    fun getSampleVideos(): List<YouTubeVideo> = sampleVideos

    fun parseYouTubeUrl(url: String): YouTubeVideo? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        val videoId = extractYouTubeId(trimmed) ?: "clip_${System.currentTimeMillis()}"
        val thumbnail = if (videoId.length == 11) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else {
            "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&q=80"
        }

        return YouTubeVideo(
            id = videoId,
            title = "YouTube Video: $videoId",
            channel = "YouTube Creator",
            durationSeconds = 180,
            viewsCount = "Imported Video",
            thumbnailUrl = thumbnail,
            videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "Video imported from $trimmed",
            category = "Imported"
        )
    }

    private fun extractYouTubeId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|/shorts/)[^#&?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) {
            matcher.group()
        } else null
    }
}
