package com.example

import com.example.media.BeatTrackRepository
import com.example.model.CinematicStyle
import com.example.model.ColorGradePreset
import com.example.model.TrackLanguage
import com.example.model.TransitionType
import com.example.model.VideoSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVideoSegmentDurationWithSpeed() {
        val normalSegment = VideoSegment(
            id = "seg1",
            startMs = 1000L,
            endMs = 5000L,
            speed = 1.0f
        )
        assertEquals(4000L, normalSegment.durationMs)

        val slowMoSegment = normalSegment.copy(speed = 0.5f)
        assertEquals(8000L, slowMoSegment.durationMs)

        val fastSegment = normalSegment.copy(speed = 2.0f)
        assertEquals(2000L, fastSegment.durationMs)
    }

    @Test
    fun testBeatTrackRepositoryContainsLanguages() {
        val hindiTracks = BeatTrackRepository.getTracksForLanguage(TrackLanguage.HINDI)
        val gujaratiTracks = BeatTrackRepository.getTracksForLanguage(TrackLanguage.GUJARATI)
        val englishTracks = BeatTrackRepository.getTracksForLanguage(TrackLanguage.ENGLISH)

        assertTrue("Should have Hindi tracks", hindiTracks.isNotEmpty())
        assertTrue("Should have Gujarati tracks", gujaratiTracks.isNotEmpty())
        assertTrue("Should have English tracks", englishTracks.isNotEmpty())

        // Verify Gujarati Garba track BPM and accent drops
        val garba = gujaratiTracks.firstOrNull { it.id == "gu_garba_dhol" }
        assertNotNull(garba)
        assertEquals(130, garba?.bpm)
        assertTrue(garba?.accentDropsMs?.isNotEmpty() == true)
    }

    @Test
    fun testColorGradeMatrixValid() {
        ColorGradePreset.values().forEach { preset ->
            assertEquals(
                "Color filter matrix must have exactly 20 elements for ${preset.name}",
                20,
                preset.colorFilterMatrix.size
            )
        }
    }

    @Test
    fun testCinematicStylesHaveValidPresets() {
        CinematicStyle.values().forEach { style ->
            assertNotNull(style.displayName)
            assertNotNull(style.recommendedColor)
            assertNotNull(style.defaultTransition)
        }
    }
}
