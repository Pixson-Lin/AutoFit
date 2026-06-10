package com.pixson.autofit.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.pixson.autofit.R

class SystemOverlayWindowHost(
    private val context: Context,
    private val windowManager: WindowManager,
    private val canDrawOverlays: () -> Boolean,
) : OverlayWindowHost {

    private var overlayView: TextView? = null

    override fun canShow(): Boolean = canDrawOverlays()

    override fun showOrUpdate(text: CharSequence) {
        if (!canShow()) return

        val view = overlayView ?: createOverlayView().also {
            overlayView = it
            windowManager.addView(it, layoutParams())
        }
        if (view.text.toString() != text.toString()) {
            view.text = text
        }
    }

    override fun dismiss() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
        overlayView = null
    }

    private fun createOverlayView(): TextView {
        return TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setBackgroundResource(R.drawable.overlay_chip_background)
            setPadding(24, 12, 24, 12)
            textSize = 12f
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }
    }
}
