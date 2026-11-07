package com.example.data.model

data class ViralMomentCandidate(
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val hook: String,
    val reason: String,
    val score: Int
) {
    init {
        require(startMs >= 0 && endMs > startMs)
        require(score in 0..100)
    }
}

/**
 * Basic deterministic ranking utility only. It does not analyze audio/video.
 * Feed candidates returned by a real transcript + Gemini analysis pipeline.
 */
object ViralMomentRanking {
    fun rank(candidates: List<ViralMomentCandidate>): List<ViralMomentCandidate> =
        candidates.sortedByDescending { it.score }.take(5)
}
