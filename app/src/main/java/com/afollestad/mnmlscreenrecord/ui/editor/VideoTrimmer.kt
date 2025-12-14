/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.mnmlscreenrecord.ui.editor

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
import java.io.File
import java.nio.ByteBuffer

internal object VideoTrimmer {

  @Throws(Exception::class)
  fun trim(
    inputPath: String,
    outputPath: String,
    startMs: Long,
    endMs: Long
  ) {
    require(startMs >= 0L) { "startMs must be >= 0" }
    require(endMs > startMs) { "endMs must be > startMs" }

    File(outputPath).parentFile?.mkdirs()

    val startUs = startMs * 1000L
    val endUs = endMs * 1000L

    val extractor = MediaExtractor()
    extractor.setDataSource(inputPath)

    val muxer = MediaMuxer(outputPath, MUXER_OUTPUT_MPEG_4)

    val trackMap = LinkedHashMap<Int, Int>()
    for (trackIndex in 0 until extractor.trackCount) {
      val format = extractor.getTrackFormat(trackIndex)
      val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
      if (mime.startsWith("audio/") || mime.startsWith("video/")) {
        val dstIndex = muxer.addTrack(format)
        trackMap[trackIndex] = dstIndex
      }
    }

    var muxerStarted = false
    try {
      muxer.start()
      muxerStarted = true

      val bufferInfo = MediaCodec.BufferInfo()

      for ((srcTrackIndex, dstTrackIndex) in trackMap) {
        val trackExtractor = MediaExtractor()
        try {
          trackExtractor.setDataSource(inputPath)
          trackExtractor.selectTrack(srcTrackIndex)
          trackExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

          val format = trackExtractor.getTrackFormat(srcTrackIndex)
          val maxSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
          } else {
            DEFAULT_BUFFER_SIZE
          }
          val buffer = ByteBuffer.allocate(maxSize)

          while (true) {
            val sampleTimeUs = trackExtractor.sampleTime
            if (sampleTimeUs < 0) break
            if (sampleTimeUs < startUs) {
              trackExtractor.advance()
              continue
            }
            if (sampleTimeUs > endUs) break

            bufferInfo.offset = 0
            bufferInfo.size = trackExtractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = sampleTimeUs - startUs
            bufferInfo.flags = trackExtractor.sampleFlags

            muxer.writeSampleData(dstTrackIndex, buffer, bufferInfo)
            trackExtractor.advance()
          }
        } finally {
          trackExtractor.release()
        }
      }
    } finally {
      extractor.release()
      if (muxerStarted) {
        try {
          muxer.stop()
        } catch (_: Throwable) {
        }
      }
      muxer.release()
    }
  }

  private const val DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024
}
