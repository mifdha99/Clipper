package com.example

import com.example.data.repository.VideoRepository
import com.example.data.repository.YouTubeParseResult
import com.example.ui.components.formatSeconds
import com.example.ui.components.formatTimeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val videoRepository = VideoRepository()

    @Test
    fun testParseYouTubeUrl() {
        val standardUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = videoRepository.parseYouTubeUrl(standardUrl)
        assertTrue(result is YouTubeParseResult.Valid)
        val valid = result as YouTubeParseResult.Valid
        assertEquals("dQw4w9WgXcQ", valid.videoId)

        val shortUrl = "https://youtu.be/jNQXAC9IVRw"
        val result2 = videoRepository.parseYouTubeUrl(shortUrl)
        assertTrue(result2 is YouTubeParseResult.Valid)
        val valid2 = result2 as YouTubeParseResult.Valid
        assertEquals("jNQXAC9IVRw", valid2.videoId)
    }

    @Test
    fun testFormatTimeMs() {
        assertEquals("00:15.5", formatTimeMs(15500L))
        assertEquals("01:30.0", formatTimeMs(90000L))
    }

    @Test
    fun testFormatSeconds() {
        assertEquals("00:45", formatSeconds(45))
        assertEquals("02:10", formatSeconds(130))
    }
}
