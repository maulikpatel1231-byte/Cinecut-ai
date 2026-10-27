package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoSource(
    val uri: Uri,
    val durationMs: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val rotation: Int = 0,
    val displayName: String = "Imported Video",
    val sizeBytes: Long = 0L,
    val thumbnailBitmap: Bitmap? = null
)

data class VideoSegment(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val speed: Float = 1.0f,
    val transition: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 400L,
    val label: String = "Cut",
    val colorGrade: ColorGradePreset = ColorGradePreset.NATURAL,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f
) {
    val durationMs: Long
        get() = ((endMs - startMs).coerceAtLeast(100L) / speed).toLong()

    val rawDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(100L)
}

enum class TransitionType(val title: String, val subtitle: String) {
    NONE("Cut", "Direct cut"),
    CROSSFADE("Crossfade", "Smooth dissolve"),
    FLASH_WHITE("White Flash", "Dramatic beat flash"),
    WHIP_PAN("Whip Pan", "High speed lateral sweep"),
    DIP_TO_BLACK("Dip to Black", "Cinematic fade"),
    GLITCH("Glitch", "Digital artifact distortion"),
    ZOOM_IN("Zoom Punch", "Dynamic impact zoom")
}

enum class ColorGradePreset(
    val title: String,
    val description: String,
    val colorFilterMatrix: FloatArray
) {
    NATURAL(
        "Original",
        "Neutral, untouched colors",
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    TEAL_ORANGE(
        "Teal & Orange",
        "Hollywood blockbuster contrast",
        floatArrayOf(
            1.25f, 0.0f, -0.1f, 0f, 15f,
            0.0f, 1.05f, 0.05f, 0f, 0f,
            -0.2f, 0.1f, 1.35f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GOLDEN_HOUR(
        "Golden Hour",
        "Warm Bollywood sunset radiance",
        floatArrayOf(
            1.3f, 0.1f, -0.1f, 0f, 20f,
            0.05f, 1.15f, 0.0f, 0f, 10f,
            -0.15f, -0.1f, 0.85f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    MOODY_NOIR(
        "Moody Noir",
        "Monochromatic mystery with deep blacks",
        floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, -15f,
            0.33f, 0.59f, 0.11f, 0f, -15f,
            0.33f, 0.59f, 0.11f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GARBA_VIBRANT(
        "Garba Vibrant",
        "Punchy festive saturation for dance",
        floatArrayOf(
            1.35f, -0.15f, -0.15f, 0f, 10f,
            -0.15f, 1.35f, -0.15f, 0f, 10f,
            -0.15f, -0.15f, 1.35f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CYBERPUNK_NEON(
        "Cyberpunk Neon",
        "Electric cyan and magenta split-tones",
        floatArrayOf(
            1.1f, -0.2f, 0.3f, 0f, 20f,
            -0.2f, 0.9f, 0.2f, 0f, -10f,
            0.3f, 0.1f, 1.4f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VINTAGE_FILM(
        "Vintage 70s",
        "Warm nostalgic film emulsion",
        floatArrayOf(
            1.1f, 0.05f, 0.0f, 0f, 15f,
            0.0f, 1.0f, 0.05f, 0f, 10f,
            0.0f, 0.05f, 0.8f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EMERALD_MATRIX(
        "Emerald Matrix",
        "Cold sci-fi cinematic green tint",
        floatArrayOf(
            0.85f, 0.15f, 0.0f, 0f, -10f,
            0.1f, 1.25f, 0.1f, 0f, 20f,
            0.0f, 0.1f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

enum class TrackLanguage(val label: String) {
    ALL("All Languages"),
    HINDI("Hindi Beats"),
    GUJARATI("Gujarati Folk/Garba"),
    ENGLISH("English Cinematic"),
    CUSTOM("Custom Audio")
}

data class BeatTrack(
    val id: String,
    val title: String,
    val language: TrackLanguage,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val durationMs: Long,
    val accentDropsMs: List<Long>,
    val customUri: String? = null
) {
    val beatIntervalMs: Long
        get() = (60_000L / bpm.coerceAtLeast(40))
}

enum class CinematicStyle(
    val displayName: String,
    val tagline: String,
    val description: String,
    val recommendedColor: ColorGradePreset,
    val defaultTransition: TransitionType
) {
    ACTION_BLOCKBUSTER(
        "Action Blockbuster",
        "High Octane • Fast Cuts • Beat Drops",
        "Splits video into rapid 1.5s - 3s punches, whip pans, and punchy contrast.",
        ColorGradePreset.TEAL_ORANGE,
        TransitionType.WHIP_PAN
    ),
    BOLLYWOOD_ROMANCE(
        "Bollywood Drama",
        "Emotional • Slow-Mo • Golden Glow",
        "Extended dramatic shots with smooth crossfades and sunset warmth.",
        ColorGradePreset.GOLDEN_HOUR,
        TransitionType.CROSSFADE
    ),
    GUJARATI_GARBA_PULSE(
        "Gujarati Garba Pulse",
        "Rhythmic • Dhol Sync • High Energy",
        "Snaps video cuts to traditional dholak & dandiya beats with vibrant flashes.",
        ColorGradePreset.GARBA_VIBRANT,
        TransitionType.FLASH_WHITE
    ),
    CYBER_THRILLER(
        "Cyberpunk Thriller",
        "Futuristic • Glitch FX • Neon Grade",
        "Digital cuts synchronized with synth bass drops and chromatic flashes.",
        ColorGradePreset.CYBERPUNK_NEON,
        TransitionType.GLITCH
    ),
    NEO_NOIR(
        "Neo-Noir Mystery",
        "Slow Burn • Deep Shadows • Monochrome",
        "Atmospheric pacing, long takes, and dramatic dip-to-black scene breaks.",
        ColorGradePreset.MOODY_NOIR,
        TransitionType.DIP_TO_BLACK
    ),
    REEL_TRENDING(
        "Shorts & Reels Trend",
        "Viral Pacing • Zoom Punches • Snappy",
        "Optimized for 9:16 vertical video with snappy 1.8s retention cuts.",
        ColorGradePreset.TEAL_ORANGE,
        TransitionType.ZOOM_IN
    )
}

enum class AspectRatioPreset(val title: String, val ratio: Float, val isScope: Boolean = false) {
    SCOPE_239("2.39:1 Cinema", 2.39f, true),
    WIDE_169("16:9 Cinema Wide", 16f / 9f),
    VERTICAL_916("9:16 Reels / Shorts", 9f / 16f),
    SQUARE_11("1:1 Square", 1.0f),
    PORTRAIT_45("4:5 Feed", 4f / 5f)
}

data class ColorAdjustments(
    val brightness: Float = 0f,      // -50f to 50f
    val contrast: Float = 1.0f,     // 0.5f to 2.0f
    val saturation: Float = 1.0f,   // 0.0f to 2.0f
    val warmth: Float = 0f,         // -30f to 30f
    val vignette: Float = 0f,       // 0f to 100f
    val letterboxScope: Boolean = true,
    val filmGrain: Boolean = false,
    val beatFlashEnabled: Boolean = true,
    val beatShakeEnabled: Boolean = true
)

data class AiThinkingStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val title: String,
    val logMessage: String,
    val reasoningDetail: String
)

sealed interface AiThinkingState {
    data object Idle : AiThinkingState
    data class Thinking(
        val step: AiThinkingStep,
        val progress: Float,
        val accumulatedLogs: List<String>
    ) : AiThinkingState
    data class Ready(
        val style: CinematicStyle,
        val suggestedSegments: List<VideoSegment>,
        val reasoningSummary: String,
        val beatDropMatches: Int
    ) : AiThinkingState
    data class Error(val message: String) : AiThinkingState
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(val progress: Float, val message: String) : ExportState
    data class Success(val outputUri: Uri, val outputPath: String, val durationMs: Long) : ExportState
    data class Error(val message: String) : ExportState
}
