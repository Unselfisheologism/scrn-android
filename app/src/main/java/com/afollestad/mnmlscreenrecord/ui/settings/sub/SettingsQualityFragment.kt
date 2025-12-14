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
package com.afollestad.mnmlscreenrecord.ui.settings.sub

import android.os.Bundle
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afollestad.mnmlscreenrecord.R
import com.afollestad.mnmlscreenrecord.common.misc.displayInfo
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_AUDIO_BIT_RATE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_FRAME_RATE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_QUALITY_PRESET
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RECORD_AUDIO
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RESOLUTION_HEIGHT
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RESOLUTION_WIDTH
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_VIDEO_BIT_RATE
import com.afollestad.mnmlscreenrecord.common.rx.attachLifecycle
import com.afollestad.mnmlscreenrecord.ui.settings.base.BaseSettingsFragment
import com.afollestad.mnmlscreenrecord.ui.settings.bitRateString
import com.afollestad.rxkprefs.Pref
import kotlin.math.max
import kotlin.math.min
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

/** @author Aidan Follestad (@afollestad) */
class SettingsQualityFragment : BaseSettingsFragment() {

  private val qualityPresetPref by inject<Pref<String>>(named(PREF_QUALITY_PRESET))
  private val frameRatePref by inject<Pref<Int>>(named(PREF_FRAME_RATE))
  private val resolutionWidthPref by inject<Pref<Int>>(named(PREF_RESOLUTION_WIDTH))
  private val resolutionHeightPref by inject<Pref<Int>>(named(PREF_RESOLUTION_HEIGHT))
  private val videoBitRatePref by inject<Pref<Int>>(named(PREF_VIDEO_BIT_RATE))
  private val audioBitRatePref by inject<Pref<Int>>(named(PREF_AUDIO_BIT_RATE))
  private val recordAudioPref by inject<Pref<Boolean>>(named(PREF_RECORD_AUDIO))

  private var isApplyingPreset: Boolean = false

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.settings_quality, rootKey)

    setupQualityPresetPref()
    setupFrameRatePref()
    setupResolutionPref()
    setupVideoBitRatePref()
    setupAudioBitRatePref()
  }

  private fun setupQualityPresetPref() {
    val presetEntry = findPreference(PREF_QUALITY_PRESET)

    presetEntry.setOnPreferenceClickListener {
      val values = resources.getStringArray(R.array.quality_preset_values)
      val currentValue = qualityPresetPref.get()
      val defaultIndex = values.indexOf(currentValue).coerceAtLeast(0)

      MaterialDialog(settingsActivity).show {
        title(R.string.setting_quality_preset)
        listItemsSingleChoice(
            res = R.array.quality_preset_options,
            initialSelection = defaultIndex
        ) { _, which, _ ->
          qualityPresetPref.set(values[which])
        }
        positiveButton(R.string.select)
      }
      true
    }

    qualityPresetPref.observe()
        .distinctUntilChanged()
        .subscribe { presetValue ->
          presetEntry.summary = presetLabelFor(presetValue)
          updateCustomPrefsEnabled(presetValue == QualityPreset.CUSTOM)

          if (!isApplyingPreset && presetValue != QualityPreset.CUSTOM) {
            isApplyingPreset = true
            applyPreset(presetValue)
            isApplyingPreset = false
          }
        }
        .attachLifecycle(this)
  }

  private fun setupFrameRatePref() {
    val frameRateEntry = findPreference(PREF_FRAME_RATE)
    frameRateEntry.setOnPreferenceClickListener {
      val rawValues = resources.getIntArray(R.array.frame_rate_values)
      val currentValue = frameRatePref.get()
      val defaultIndex = rawValues.indexOf(currentValue)

      MaterialDialog(settingsActivity).show {
        title(R.string.setting_framerate)
        listItemsSingleChoice(
            res = R.array.frame_rate_options,
            initialSelection = defaultIndex
        ) { _, which, _ ->
          frameRatePref.set(rawValues[which])
          maybeSetPresetCustom()
        }
        positiveButton(R.string.select)
      }
      true
    }
    frameRatePref.observe()
        .distinctUntilChanged()
        .subscribe {
          frameRateEntry.summary = getString(R.string.setting_framerate_desc, it)
        }
        .attachLifecycle(this)
  }

  private fun setupResolutionPref() {
    val resolutionEntry = findPreference(KEY_RESOLUTION)

    resolutionEntry.setOnPreferenceClickListener {
      val (currentW, currentH) = currentResolutionSetting()
      val defaultIndex = resolutionOptions.indexOfFirst { it.width == currentW && it.height == currentH }
          .coerceAtLeast(0)

      MaterialDialog(settingsActivity).show {
        title(R.string.setting_resolution)
        listItemsSingleChoice(
            items = resolutionOptions.map { it.label },
            initialSelection = defaultIndex
        ) { _, which, _ ->
          val selected = resolutionOptions[which]
          resolutionWidthPref.set(selected.width)
          resolutionHeightPref.set(selected.height)
          maybeSetPresetCustom()
        }
        positiveButton(R.string.select)
      }
      true
    }

    resolutionWidthPref.observe()
        .distinctUntilChanged()
        .subscribe { updateResolutionSummary(resolutionEntry) }
        .attachLifecycle(this)

    resolutionHeightPref.observe()
        .distinctUntilChanged()
        .subscribe { updateResolutionSummary(resolutionEntry) }
        .attachLifecycle(this)
  }

  private fun setupVideoBitRatePref() {
    val videoBitRateEntry = findPreference(PREF_VIDEO_BIT_RATE)
    videoBitRateEntry.setOnPreferenceClickListener {
      val rawValues = resources.getIntArray(R.array.bit_rate_values)
      val currentValue = videoBitRatePref.get()
      val defaultIndex = rawValues.indexOf(currentValue)

      MaterialDialog(settingsActivity).show {
        title(R.string.setting_bitrate)
        listItemsSingleChoice(
            res = R.array.bit_rate_options,
            initialSelection = defaultIndex
        ) { _, which, _ ->
          videoBitRatePref.set(rawValues[which])
          maybeSetPresetCustom()
        }
        positiveButton(R.string.select)
      }
      true
    }
    videoBitRatePref.observe()
        .distinctUntilChanged()
        .subscribe {
          videoBitRateEntry.summary = getString(R.string.setting_bitrate_desc, it.bitRateString())
        }
        .attachLifecycle(this)
  }

  private fun setupAudioBitRatePref() {
    val audioBitRateEntry = findPreference(PREF_AUDIO_BIT_RATE)
    audioBitRateEntry.isVisible = recordAudioPref.get()

    audioBitRateEntry.setOnPreferenceClickListener {
      val context = activity ?: return@setOnPreferenceClickListener false
      val rawValues = resources.getIntArray(R.array.audio_bit_rate_values)
      val currentValue = audioBitRatePref.get()
      val defaultIndex = rawValues.indexOf(currentValue)

      MaterialDialog(context).show {
        title(R.string.setting_audio_bitrate)
        listItemsSingleChoice(
            res = R.array.audio_bit_rate_options,
            initialSelection = defaultIndex
        ) { _, which, _ ->
          audioBitRatePref.set(rawValues[which])
        }
        positiveButton(R.string.select)
      }
      true
    }
    audioBitRatePref.observe()
        .distinctUntilChanged()
        .subscribe {
          audioBitRateEntry.summary = getString(R.string.setting_audio_bitrate_desc, it.bitRateString())
        }
        .attachLifecycle(this)
  }

  private fun applyPreset(presetValue: String) {
    val preset = when (presetValue) {
      QualityPreset.LOW -> PresetConfig(width = 480, height = 854, frameRate = 30, videoBitRate = 2_000_000)
      QualityPreset.MEDIUM -> PresetConfig(width = 720, height = 1280, frameRate = 30, videoBitRate = 5_000_000)
      QualityPreset.HIGH -> PresetConfig(width = 1080, height = 1920, frameRate = 60, videoBitRate = 10_000_000)
      else -> return
    }

    val displayInfo = settingsActivity.windowManager.displayInfo()
    val portraitWidth = min(displayInfo.width, displayInfo.height)
    val portraitHeight = max(displayInfo.width, displayInfo.height)

    val useScreenResolution = preset.width > portraitWidth || preset.height > portraitHeight
    resolutionWidthPref.set(if (useScreenResolution) 0 else preset.width)
    resolutionHeightPref.set(if (useScreenResolution) 0 else preset.height)

    frameRatePref.set(preset.frameRate)
    videoBitRatePref.set(preset.videoBitRate)
  }

  private fun maybeSetPresetCustom() {
    if (isApplyingPreset) return
    if (qualityPresetPref.get() != QualityPreset.CUSTOM) {
      qualityPresetPref.set(QualityPreset.CUSTOM)
    }
  }

  private fun updateCustomPrefsEnabled(isCustom: Boolean) {
    findPreference(KEY_RESOLUTION).isEnabled = isCustom
    findPreference(PREF_FRAME_RATE).isEnabled = isCustom
    findPreference(PREF_VIDEO_BIT_RATE).isEnabled = isCustom
  }

  private fun presetLabelFor(value: String): String {
    val values = resources.getStringArray(R.array.quality_preset_values)
    val labels = resources.getStringArray(R.array.quality_preset_options)

    val index = values.indexOf(value)
    return if (index != -1) labels[index] else labels[0]
  }

  private fun updateResolutionSummary(resolutionEntry: Preference) {
    val (w, h) = currentResolutionSetting()
    resolutionEntry.summary = if (w == 0 || h == 0) {
      resources.getString(R.string.setting_resolution_current_screen)
    } else {
      resources.getString(R.string.setting_resolution_desc, w, h)
    }
  }

  private fun currentResolutionSetting(): Pair<Int, Int> {
    return resolutionWidthPref.get() to resolutionHeightPref.get()
  }

  private data class PresetConfig(
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val videoBitRate: Int
  )

  private data class ResolutionOption(
    val label: String,
    val width: Int,
    val height: Int
  )

  private val resolutionOptions by lazy {
    listOf(
        ResolutionOption(getString(R.string.use_screen_resolution), 0, 0),
        ResolutionOption("480p", 480, 854),
        ResolutionOption("720p", 720, 1280),
        ResolutionOption("1080p", 1080, 1920)
    )
  }

  private object QualityPreset {
    const val CUSTOM = "custom"
    const val LOW = "low"
    const val MEDIUM = "medium"
    const val HIGH = "high"
  }

  private companion object {
    private const val KEY_RESOLUTION = "resolution"
  }
}
