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

import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.mnmlscreenrecord.R
import com.afollestad.mnmlscreenrecord.common.misc.timestampString
import com.afollestad.mnmlscreenrecord.common.misc.toast
import com.afollestad.mnmlscreenrecord.databinding.ActivityEditorBinding
import com.afollestad.mnmlscreenrecord.engine.recordings.Recording
import com.afollestad.mnmlscreenrecord.engine.recordings.RecordingScanner
import com.afollestad.mnmlscreenrecord.theming.DarkModeSwitchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.util.Date

class EditorActivity : DarkModeSwitchActivity() {

  private val recordingScanner by inject<RecordingScanner>()
  private lateinit var binding: ActivityEditorBinding

  private lateinit var recording: Recording
  private var durationMs: Long = 0L

  private var startMs: Long = 0L
  private var endMs: Long = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityEditorBinding.inflate(layoutInflater)
    setContentView(binding.root)

    recording = intent.getParcelableExtra(EXTRA_RECORDING)
        ?: throw IllegalStateException("Missing EXTRA_RECORDING")

    setupToolbar()
    setupVideo()
    setupTrimControls()
  }

  private fun setupToolbar() {
    setSupportActionBar(binding.includeAppbar.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.includeAppbar.toolbar.setNavigationOnClickListener { finish() }

    binding.includeAppbar.toolbarTitle.text = getString(R.string.editor_title)
  }

  private fun setupVideo() {
    binding.video.setVideoPath(recording.path)
    binding.video.setOnPreparedListener { player ->
      durationMs = player.duration.toLong()
      startMs = 0L
      endMs = durationMs
      updateLabels()
      binding.startSeek.max = durationMs.toInt()
      binding.endSeek.max = durationMs.toInt()
      binding.endSeek.progress = durationMs.toInt()
    }

    if (durationMs <= 0L) {
      durationMs = loadDurationMs(recording.path)
      if (durationMs > 0L) {
        binding.startSeek.max = durationMs.toInt()
        binding.endSeek.max = durationMs.toInt()
        binding.endSeek.progress = durationMs.toInt()
        startMs = 0L
        endMs = durationMs
        updateLabels()
      }
    }

    binding.playPause.setOnClickListener {
      if (binding.video.isPlaying) {
        binding.video.pause()
        binding.playPause.setText(R.string.editor_play)
      } else {
        if (binding.video.currentPosition < startMs || binding.video.currentPosition > endMs) {
          binding.video.seekTo(startMs.toInt())
        }
        binding.video.start()
        binding.playPause.setText(R.string.editor_pause)
      }
    }
  }

  private fun setupTrimControls() {
    binding.startSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        startMs = progress.toLong().coerceAtMost(endMs)
        updateLabels()
        binding.video.seekTo(startMs.toInt())
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

      override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })

    binding.endSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        endMs = progress.toLong().coerceAtLeast(startMs)
        updateLabels()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

      override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })

    binding.export.setOnClickListener { exportTrimmed() }
  }

  private fun exportTrimmed() {
    val trimDuration = endMs - startMs
    if (trimDuration <= 0L) {
      toast(R.string.editor_invalid_range)
      return
    }

    val outputFile = createOutputFile(recording)

    val dialog = MaterialDialog(this).show {
      title(R.string.editor_export)
      message(R.string.editor_exporting)
      cancelable(false)
      cancelOnTouchOutside(false)
    }

    binding.export.isEnabled = false
    binding.startSeek.isEnabled = false
    binding.endSeek.isEnabled = false

    GlobalScope.launch(Dispatchers.IO) {
      try {
        VideoTrimmer.trim(
            inputPath = recording.path,
            outputPath = outputFile.absolutePath,
            startMs = startMs,
            endMs = endMs
        )

        recordingScanner.scan(outputFile) {
          runOnUiThread {
            dialog.dismiss()
            toast(R.string.editor_export_success)
            finish()
          }
        }
      } catch (e: Exception) {
        runOnUiThread {
          dialog.dismiss()
          binding.export.isEnabled = true
          binding.startSeek.isEnabled = true
          binding.endSeek.isEnabled = true
          MaterialDialog(this@EditorActivity).show {
            title(text = "Error")
            message(text = e.message ?: e.toString())
            positiveButton(android.R.string.ok)
          }
        }
      }
    }
  }

  private fun createOutputFile(recording: Recording): File {
    val parent = File(recording.path).parentFile ?: filesDir
    val now = Date().timestampString()
    return File(parent, "MNML-$now-edited.mp4")
  }

  private fun updateLabels() {
    binding.startLabel.text = getString(R.string.editor_start_time, startMs.formatDuration())
    binding.endLabel.text = getString(R.string.editor_end_time, endMs.formatDuration())
  }

  private fun loadDurationMs(path: String): Long {
    return try {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(path)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    } catch (_: Throwable) {
      0L
    }
  }

  private fun Long.formatDuration(): String {
    val totalSeconds = (this / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
  }

  companion object {
    const val EXTRA_RECORDING = "recording"
  }
}
