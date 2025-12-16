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

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.os.Bundle
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afollestad.mnmlscreenrecord.R
import com.afollestad.mnmlscreenrecord.common.misc.toast
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_ALWAYS_SHOW_CONTROLS
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_AUDIO_BIT_RATE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_COUNTDOWN
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_DARK_MODE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_DARK_MODE_AUTOMATIC
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_DARK_MODE_END
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_DARK_MODE_START
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_FRAME_RATE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_MAX_DURATION_MINUTES
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_QUALITY_PRESET
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RECORDINGS_FOLDER
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RECORD_AUDIO
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RESOLUTION_HEIGHT
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_RESOLUTION_WIDTH
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_STOP_ON_SCREEN_OFF
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_STOP_ON_SHAKE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_VIDEO_BIT_RATE
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_WATERMARK_ENABLED
import com.afollestad.mnmlscreenrecord.common.prefs.PrefNames.PREF_WATERMARK_TEXT
import com.afollestad.mnmlscreenrecord.ui.settings.base.BaseSettingsFragment
import org.json.JSONObject

class SettingsPresetsFragment : BaseSettingsFragment() {

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.settings_presets, rootKey)

    findPreference(KEY_SAVE_PRESET).setOnPreferenceClickListener {
      promptSavePreset()
      true
    }

    findPreference(KEY_APPLY_PRESET).setOnPreferenceClickListener {
      promptChoosePreset { preset ->
        applyPreset(preset)
      }
      true
    }

    findPreference(KEY_SHARE_PRESET).setOnPreferenceClickListener {
      promptChoosePreset { preset ->
        sharePreset(preset)
      }
      true
    }

    findPreference(KEY_DELETE_PRESET).setOnPreferenceClickListener {
      promptChoosePreset { preset ->
        deletePreset(preset)
      }
      true
    }
  }

  private fun promptSavePreset() {
    val dialog = MaterialDialog(settingsActivity).show {
      title(R.string.setting_save_preset)
      customView(R.layout.dialog_text_input)
      positiveButton(R.string.select) {
        val customView = getCustomView() ?: return@positiveButton
        val input = customView.findViewById<EditText>(R.id.input)
        val name = input.text.toString().trim()
        if (name.isEmpty()) return@positiveButton

        savePreset(name, exportCurrentSettings())
      }
    }

    dialog.getCustomView()?.findViewById<EditText>(R.id.input)?.hint = getString(R.string.setting_preset_name)
  }

  private fun promptChoosePreset(onSelected: (Preset) -> Unit) {
    val presets = loadPresets().sortedBy { it.name.toLowerCase() }
    if (presets.isEmpty()) {
      settingsActivity.toast(R.string.setting_no_presets)
      return
    }

    MaterialDialog(settingsActivity).show {
      title(R.string.setting_category_presets)
      listItemsSingleChoice(items = presets.map { it.name }) { _, which, _ ->
        onSelected(presets[which])
      }
      positiveButton(R.string.select)
    }
  }

  private fun exportCurrentSettings(): JSONObject {
    val prefs = settingsActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    val all = prefs.all

    val result = JSONObject()
    for (key in EXPORTED_KEYS) {
      val value = all[key] ?: continue
      when (value) {
        is Boolean -> result.put(key, value)
        is Int -> result.put(key, value)
        is Long -> result.put(key, value)
        is Float -> result.put(key, value)
        is String -> result.put(key, value)
      }
    }

    return result
  }

  private fun applyPreset(preset: Preset) {
    val prefs = settingsActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    val editor = prefs.edit()

    for (key in EXPORTED_KEYS) {
      if (!preset.json.has(key)) continue
      val value = preset.json.get(key)
      when (value) {
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Double -> editor.putInt(key, value.toInt())
        is String -> editor.putString(key, value)
      }
    }

    editor.apply()
    settingsActivity.recreate()
  }

  private fun sharePreset(preset: Preset) {
    val jsonString = preset.json.toString(2)
    startActivity(
        Intent.createChooser(
            Intent(ACTION_SEND).apply {
              type = "text/plain"
              putExtra(EXTRA_TEXT, jsonString)
            },
            getString(R.string.setting_share_preset)
        )
    )
  }

  private fun deletePreset(preset: Preset) {
    val obj = loadPresetsJson()
    obj.remove(preset.name)
    savePresetsJson(obj)
  }

  private fun savePreset(
    name: String,
    json: JSONObject
  ) {
    val obj = loadPresetsJson()
    obj.put(name, json)
    savePresetsJson(obj)
  }

  private fun loadPresets(): List<Preset> {
    val obj = loadPresetsJson()
    val result = mutableListOf<Preset>()

    val names = obj.keys()
    while (names.hasNext()) {
      val name = names.next()
      val json = obj.optJSONObject(name) ?: continue
      result.add(Preset(name, json))
    }

    return result
  }

  private fun loadPresetsJson(): JSONObject {
    val prefs = settingsActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_PRESETS, null) ?: return JSONObject()
    return try {
      JSONObject(raw)
    } catch (_: Throwable) {
      JSONObject()
    }
  }

  private fun savePresetsJson(json: JSONObject) {
    settingsActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        .edit()
        .putString(KEY_SAVED_PRESETS, json.toString())
        .apply()
  }

  private data class Preset(
    val name: String,
    val json: JSONObject
  )

  private companion object {
    private const val PREFS_NAME = "settings"

    private const val KEY_SAVED_PRESETS = "saved_presets"

    private const val KEY_SAVE_PRESET = "save_preset"
    private const val KEY_APPLY_PRESET = "apply_preset"
    private const val KEY_SHARE_PRESET = "share_preset"
    private const val KEY_DELETE_PRESET = "delete_preset"

    private val EXPORTED_KEYS = listOf(
        PREF_QUALITY_PRESET,
        PREF_FRAME_RATE,
        PREF_RESOLUTION_WIDTH,
        PREF_RESOLUTION_HEIGHT,
        PREF_VIDEO_BIT_RATE,
        PREF_AUDIO_BIT_RATE,
        PREF_RECORD_AUDIO,
        PREF_COUNTDOWN,
        PREF_MAX_DURATION_MINUTES,
        PREF_RECORDINGS_FOLDER,
        PREF_DARK_MODE,
        PREF_DARK_MODE_AUTOMATIC,
        PREF_DARK_MODE_START,
        PREF_DARK_MODE_END,
        PREF_WATERMARK_ENABLED,
        PREF_WATERMARK_TEXT,
        PREF_STOP_ON_SCREEN_OFF,
        PREF_STOP_ON_SHAKE,
        PREF_ALWAYS_SHOW_CONTROLS
    )
  }
}
