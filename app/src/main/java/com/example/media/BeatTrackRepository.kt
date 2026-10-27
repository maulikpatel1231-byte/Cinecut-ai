package com.example.media

import com.example.model.BeatTrack
import com.example.model.TrackLanguage

object BeatTrackRepository {

    val defaultTracks: List<BeatTrack> = listOf(
        // HINDI TRACKS
        BeatTrack(
            id = "hi_action_drop",
            title = "Dhamaka 808 Action Beat",
            language = TrackLanguage.HINDI,
            artist = "Mumbai Cine Beats",
            genre = "Bollywood Action Trap",
            bpm = 128,
            durationMs = 60_000L,
            accentDropsMs = listOf(4000L, 8000L, 12000L, 16000L, 24000L, 32000L, 40000L, 48000L)
        ),
        BeatTrack(
            id = "hi_romantic_chill",
            title = "Kesariya Sufi Sunset",
            language = TrackLanguage.HINDI,
            artist = "Desi Studio Collective",
            genre = "Bollywood Romance Lo-Fi",
            bpm = 92,
            durationMs = 60_000L,
            accentDropsMs = listOf(6521L, 13043L, 19565L, 26086L, 39130L, 52173L)
        ),
        BeatTrack(
            id = "hi_gully_drill",
            title = "Gully Rap Street Bass",
            language = TrackLanguage.HINDI,
            artist = "Bandra Sound Lab",
            genre = "Desi Hip Hop / Drill",
            bpm = 140,
            durationMs = 60_000L,
            accentDropsMs = listOf(3428L, 6857L, 10285L, 17142L, 24000L, 30857L, 41142L)
        ),
        BeatTrack(
            id = "hi_dhol_fusion",
            title = "Punjab To Mumbai Dholak",
            language = TrackLanguage.HINDI,
            artist = "T-Pulse Producers",
            genre = "Bhangra Bass Drop",
            bpm = 132,
            durationMs = 60_000L,
            accentDropsMs = listOf(3636L, 7272L, 10909L, 14545L, 21818L, 29090L, 36363L)
        ),

        // GUJARATI TRACKS
        BeatTrack(
            id = "gu_garba_dhol",
            title = "Garba Trance Dholak 130",
            language = TrackLanguage.GUJARATI,
            artist = "Amdavad Beats",
            genre = "Navratri Garba Drop",
            bpm = 130,
            durationMs = 60_000L,
            accentDropsMs = listOf(3692L, 7384L, 11076L, 14769L, 22153L, 29538L, 36923L, 44307L)
        ),
        BeatTrack(
            id = "gu_dandiya_speed",
            title = "Dandiya Fast Beat Clapper",
            language = TrackLanguage.GUJARATI,
            artist = "Rajkot Folk Syndicate",
            genre = "High Speed Dandiya",
            bpm = 144,
            durationMs = 60_000L,
            accentDropsMs = listOf(3333L, 6666L, 9999L, 13333L, 19999L, 26666L, 33333L)
        ),
        BeatTrack(
            id = "gu_kathiyawadi_bass",
            title = "Kathiyawadi Bass Taal",
            language = TrackLanguage.GUJARATI,
            artist = "Saurashtra Rhythms",
            genre = "Folk Bass Electronic",
            bpm = 122,
            durationMs = 60_000L,
            accentDropsMs = listOf(3934L, 7868L, 11803L, 15737L, 23606L, 31475L)
        ),
        BeatTrack(
            id = "gu_urban_pop",
            title = "Urban Gujarati Melody Pulse",
            language = TrackLanguage.GUJARATI,
            artist = "Surat Wave",
            genre = "Modern Gujarati Pop",
            bpm = 116,
            durationMs = 60_000L,
            accentDropsMs = listOf(4137L, 8275L, 12413L, 16551L, 24827L, 33103L)
        ),

        // ENGLISH CINEMATIC TRACKS
        BeatTrack(
            id = "en_epic_trailer",
            title = "Epic Cinema War Horns",
            language = TrackLanguage.ENGLISH,
            artist = "Hans Cinematic Orchestra",
            genre = "Blockbuster Hybrid Trailer",
            bpm = 110,
            durationMs = 60_000L,
            accentDropsMs = listOf(4363L, 8727L, 13090L, 17454L, 26181L, 34909L, 43636L)
        ),
        BeatTrack(
            id = "en_cyber_drift",
            title = "Cyberpunk Drift Neon Synth",
            language = TrackLanguage.ENGLISH,
            artist = "Night City Audio",
            genre = "Synthwave / Dark Electro",
            bpm = 126,
            durationMs = 60_000L,
            accentDropsMs = listOf(3809L, 7619L, 11428L, 15238L, 22857L, 30476L, 38095L)
        ),
        BeatTrack(
            id = "en_phonk_climax",
            title = "Tokyo Phonk Heavy Kick",
            language = TrackLanguage.ENGLISH,
            artist = "Shadow Drift Lab",
            genre = "Drift Phonk / Reel Hype",
            bpm = 150,
            durationMs = 60_000L,
            accentDropsMs = listOf(3200L, 6400L, 9600L, 12800L, 19200L, 25600L, 32000L)
        ),
        BeatTrack(
            id = "en_lofi_sunset",
            title = "Midnight Tape Lo-Fi Chill",
            language = TrackLanguage.ENGLISH,
            artist = "ChillHop Sessions",
            genre = "Aesthetic Lo-Fi Beat",
            bpm = 84,
            durationMs = 60_000L,
            accentDropsMs = listOf(7142L, 14285L, 21428L, 28571L, 42857L, 57142L)
        )
    )

    fun getTracksForLanguage(language: TrackLanguage): List<BeatTrack> {
        return if (language == TrackLanguage.ALL) {
            defaultTracks
        } else {
            defaultTracks.filter { it.language == language }
        }
    }
}
