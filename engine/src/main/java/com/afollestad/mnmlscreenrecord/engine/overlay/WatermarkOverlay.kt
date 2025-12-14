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
package com.afollestad.mnmlscreenrecord.engine.overlay

import android.annotation.SuppressLint
import android.graphics.PixelFormat.TRANSLUCENT
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
import android.widget.TextView
import com.afollestad.mnmlscreenrecord.common.misc.inflateAs
import com.afollestad.mnmlscreenrecord.common.providers.SdkProvider
import com.afollestad.mnmlscreenrecord.engine.R

interface WatermarkOverlay {
  fun show(text: String)

  fun hide()

  fun isShowing(): Boolean
}

class RealWatermarkOverlay(
  private val windowManager: WindowManager,
  private val layoutInflater: LayoutInflater,
  private val sdkProvider: SdkProvider
) : WatermarkOverlay {

  private var view: TextView? = null

  override fun show(text: String) {
    if (view != null) return

    val watermarkView: TextView = layoutInflater.inflateAs(R.layout.watermark_textview)
    watermarkView.text = text

    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    val type = if (sdkProvider.hasAndroidO()) {
      TYPE_APPLICATION_OVERLAY
    } else {
      TYPE_SYSTEM_OVERLAY
    }

    val params = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        type,
        FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE,
        TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.END
      x = 16
      y = 16
    }

    windowManager.addView(watermarkView, params)
    view = watermarkView
  }

  override fun hide() {
    val v = view ?: return
    try {
      windowManager.removeView(v)
    } catch (_: Throwable) {
    }
    view = null
  }

  override fun isShowing(): Boolean = view != null
}
