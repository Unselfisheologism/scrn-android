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
import com.afollestad.mnmlscreenrecord.engine.recordings.Recording
import com.afollestad.mnmlscreenrecord.engine.recordings.RecordingScanner
import com.afollestad.mnmlscreenrecord.theming.DarkModeSwitchActivity
import java.io.File
import java.util.Date
import kotlinx.android.synthetic.main.activity_editor.endLabel
import kotlinx.android.synthetic.main.activity_editor.endSeek
import kotlinx.android.synthetic.main.activity_editor.export
import kotlinx.android.synthetic.main.activity_editor.playPause
import kotlinx.android.synthetic.main.activity_editor.startLabel
import kotlinx.android.synthetic.main.activity_editor.startSeek
import kotlinx.android.synthetic.main.activity_editor.video
import kotlinx.android.synthetic.main.include_appbar.toolbar
import kotlinx.android.synthetic.main.include_appbar.toolbar_title as toolbarTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class EditorActivity : DarkModeSwitchActivity() {

  private val recordingScanner by inject<RecordingScanner>()

  private lateinit var recording: Recording
  private var durationMs: Long = 0L

  private var startMs: Long = 0L
  private var endMs: Long = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_editor)

    recording = intent.getParcelableExtra(EXTRA_RECORDING)
        ?: throw IllegalStateException("Missing EXTRA_RECORDING")

    setupToolbar()
    setupVideo()
    setupTrimControls()
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { finish() }

    toolbarTitle.text = getString(R.string.editor_title)
  }

  private fun setupVideo() {
    video.setVideoPath(recording.path)
    video.setOnPreparedListener { player ->
      durationMs = player.duration.toLong()
      startMs = 0L
      endMs = durationMs
      updateLabels()
      startSeek.max = durationMs.toInt()
      endSeek.max = durationMs.toInt()
      endSeek.progress = durationMs.toInt()
    }

    if (durationMs <= 0L) {
      durationMs = loadDurationMs(recording.path)
      if (durationMs > 0L) {
        startSeek.max = durationMs.toInt()
        endSeek.max = durationMs.toInt()
        endSeek.progress = durationMs.toInt()
        startMs = 0L
        endMs = durationMs
        updateLabels()
      }
    }

    playPause.setOnClickListener {
      if (video.isPlaying) {
        video.pause()
        playPause.setText(R.string.editor_play)
      } else {
        if (video.currentPosition < startMs || video.currentPosition > endMs) {
          video.seekTo(startMs.toInt())
        }
        video.start()
        playPause.setText(R.string.editor_pause)
      }
    }
  }

  private fun setupTrimControls() {
    startSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        startMs = progress.toLong().coerceAtMost(endMs)
        updateLabels()
        video.seekTo(startMs.toInt())
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

      override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })

    endSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        endMs = progress.toLong().coerceAtLeast(startMs)
        updateLabels()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

      override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })

    export.setOnClickListener { exportTrimmed() }
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

    export.isEnabled = false
    startSeek.isEnabled = false
    endSeek.isEnabled = false

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
          export.isEnabled = true
          startSeek.isEnabled = true
          endSeek.isEnabled = true
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
    startLabel.text = getString(R.string.editor_start_time, startMs.formatDuration())
    endLabel.text = getString(R.string.editor_end_time, endMs.formatDuration())
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
