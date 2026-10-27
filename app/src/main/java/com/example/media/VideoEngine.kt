package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.ColorAdjustments
import com.example.model.VideoSegment
import com.example.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

private const val TAG = "VideoEngine"

object VideoEngine {

    suspend fun loadVideoMetadata(context: Context, uri: Uri): VideoSource? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val width = widthStr?.toIntOrNull() ?: 1920

            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val height = heightStr?.toIntOrNull() ?: 1080

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // Extract high quality thumbnail frame at 1s or midpoint
            val timeToExtract = if (durationMs > 2000L) 1_000_000L else 0L
            val thumbnail = retriever.getFrameAtTime(
                timeToExtract,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            var displayName = "Imported_Video.mp4"
            var sizeBytes = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            VideoSource(
                uri = uri,
                durationMs = durationMs.coerceAtLeast(1000L),
                width = width,
                height = height,
                rotation = rotation,
                displayName = displayName,
                sizeBytes = sizeBytes,
                thumbnailBitmap = thumbnail
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video metadata from $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun extractThumbnailAt(context: Context, uri: Uri, timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail at $timeMs ms", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Muxes and merges the user's selected video segments with applied cuts into a final MP4 file.
     * Uses Android's native MediaExtractor + MediaMuxer pipeline.
     */
    suspend fun exportAndMerge(
        context: Context,
        source: VideoSource,
        segments: List<VideoSegment>,
        colorAdjustments: ColorAdjustments,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (segments.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No segments to merge"))
        }

        try {
            onProgress(0.05f, "Preparing video segments...")

            // Copy content URI to temporary local file for fast MediaExtractor seeking
            val tempSourceFile = File(context.cacheDir, "source_input_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                FileOutputStream(tempSourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Cannot read source video"))

            val outputFileName = "CineCut_${System.currentTimeMillis()}.mp4"
            val outputFile = File(context.cacheDir, outputFileName)

            val extractor = MediaExtractor()
            extractor.setDataSource(tempSourceFile.absolutePath)

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(source.rotation)

            val trackMap = mutableMapOf<Int, Int>() // extractorTrackIndex -> muxerTrackIndex
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    val muxerIndex = muxer.addTrack(format)
                    trackMap[i] = muxerIndex
                    videoTrackIndex = i
                } else if (mime.startsWith("audio/")) {
                    val muxerIndex = muxer.addTrack(format)
                    trackMap[i] = muxerIndex
                    audioTrackIndex = i
                }
            }

            if (trackMap.isEmpty()) {
                extractor.release()
                tempSourceFile.delete()
                return@withContext Result.failure(IllegalStateException("No valid video or audio tracks found"))
            }

            muxer.start()

            val maxBufferSize = 2 * 1024 * 1024 // 2MB buffer
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var globalVideoPtsUs = 0L
            var globalAudioPtsUs = 0L
            val totalSegments = segments.size

            for ((segIndex, segment) in segments.withIndex()) {
                val progressBase = 0.1f + (0.75f * (segIndex.toFloat() / totalSegments))
                onProgress(
                    progressBase,
                    "Merging Cut ${segIndex + 1}/$totalSegments (${segment.label}) [${segment.transition.title}]..."
                )

                val startUs = segment.startMs * 1000L
                val endUs = segment.endMs * 1000L

                // Extract Video & Audio samples for this segment
                for ((extractorIndex, muxerIndex) in trackMap) {
                    extractor.unselectTrack(videoTrackIndex)
                    if (audioTrackIndex != -1) extractor.unselectTrack(audioTrackIndex)
                    extractor.selectTrack(extractorIndex)
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    val isVideo = (extractorIndex == videoTrackIndex)
                    var firstSamplePtsUs = -1L
                    var lastSamplePtsUs = 0L

                    while (true) {
                        bufferInfo.offset = 0
                        bufferInfo.size = extractor.readSampleData(buffer, 0)
                        if (bufferInfo.size < 0) {
                            break
                        }

                        val sampleTimeUs = extractor.sampleTime
                        if (sampleTimeUs > endUs) {
                            break
                        }

                        if (sampleTimeUs >= (startUs - 200_000L)) { // include sync keyframe slightly before
                            if (firstSamplePtsUs == -1L) {
                                firstSamplePtsUs = sampleTimeUs
                            }

                            val relativePts = (sampleTimeUs - firstSamplePtsUs).coerceAtLeast(0L)
                            val finalPts = if (isVideo) {
                                globalVideoPtsUs + relativePts
                            } else {
                                globalAudioPtsUs + relativePts
                            }

                            bufferInfo.presentationTimeUs = finalPts
                            bufferInfo.flags = extractor.sampleFlags

                            muxer.writeSampleData(muxerIndex, buffer, bufferInfo)
                            lastSamplePtsUs = relativePts
                        }

                        if (!extractor.advance()) {
                            break
                        }
                    }

                    if (isVideo) {
                        globalVideoPtsUs += (lastSamplePtsUs + 33_333L) // ~30fps offset
                    } else {
                        globalAudioPtsUs += (lastSamplePtsUs + 23_220L) // audio frame offset
                    }
                }
            }

            onProgress(0.88f, "Finalizing container and metadata...")

            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing muxer", e)
            }

            try {
                extractor.release()
            } catch (e: Exception) {
                // Ignore
            }

            tempSourceFile.delete()

            onProgress(0.94f, "Saving to Gallery / Movies...")

            // Save copy to MediaStore Gallery
            saveToGallery(context, outputFile, outputFileName)

            onProgress(1.0f, "Render complete!")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error in exportAndMerge", e)
            Result.failure(e)
        }
    }

    private fun saveToGallery(context: Context, file: File, title: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.DISPLAY_NAME, title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CineCutAI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = context.contentResolver.insert(collection, values)
            if (itemUri != null) {
                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, values, null, null)
                }
                Log.d(TAG, "Successfully exported video to Gallery: $itemUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not insert to MediaStore (falling back to cached file)", e)
        }
    }
}
