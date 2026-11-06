package com.example

import com.example.data.repository.VideoRepository
import com.example.ui.components.formatSeconds
import com.example.ui.components.formatTimeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    private val videoRepository = VideoRepository()

    @Test
    fun testParseYouTubeUrl() {
        val standardUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val video = videoRepository.parseYouTubeUrl(standardUrl)
        assertNotNull(video)
        assertEquals("dQw4w9WgXcQ", video?.id)

        val shortUrl = "https://youtu.be/jNQXAC9IVRw"
        val video2 = videoRepository.parseYouTubeUrl(shortUrl)
        assertNotNull(video2)
        assertEquals("jNQXAC9IVRw", video2?.id)
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
