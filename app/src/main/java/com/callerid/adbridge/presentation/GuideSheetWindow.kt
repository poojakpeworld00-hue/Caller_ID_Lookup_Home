package com.callerid.adbridge.presentation

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.callerid.numberlookup.home.R
import com.callerid.numberlookup.home.util.GuardRail

/**
 * The guide card drawn as a real overlay window instead of an activity.
 *
 * [GuideSheetActivity] can only land on top of a system page that stays in this
 * app's task. That holds for ACTION_MANAGE_OVERLAY_PERMISSION, but the "Default
 * home app" list is normally hoisted into the Settings app's own task, and an
 * activity started right after it ends up behind it — the hint is there, just
 * never visible. A TYPE_APPLICATION_OVERLAY window has no such problem.
 *
 * Only the card is added, not the dimmed full-screen root: the window is
 * bottom-anchored and wrap-content, so everything above it — the list the user
 * has to tap — keeps receiving touches.
 *
 * Needs the "display over other apps" permission, so [show] returns false when it
 * is missing and the caller falls back to the activity.
 */
object GuideSheetWindow {

    private const val AUTO_DISMISS_MS = 3_000L
    private const val POLL_MS = 500L

    private val main = Handler(Looper.getMainLooper())
    private var shown: View? = null

    /** True when the card was added; false when the caller should fall back. */
    fun show(context: Context, mode: String): Boolean {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return false

        return runCatching {
            dismiss()

            val inflated = LayoutInflater.from(app).inflate(R.layout.screen_overlay_guide, null)
            val card = inflated.findViewById<View>(R.id.overlayCardVw)
            (card.parent as? ViewGroup)?.removeView(card)
            GuideSheetActivity.applyMode(card, mode)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // NOT_FOCUSABLE keeps the keyboard and back key with the page underneath;
                // NOT_TOUCH_MODAL lets every touch outside the card through to it.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.BOTTOM }

            val windowManager = app.getSystemService(WindowManager::class.java)
            windowManager.addView(card, params)
            shown = card

            // Same nav-bar clearance the activity applies, for the same reason.
            ViewCompat.setOnApplyWindowInsetsListener(card) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, v.paddingTop, bars.right, bars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(card)

            card.alpha = 0f
            card.post {
                card.translationY = card.height.toFloat()
                card.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(280L)
                    .setInterpolator(DecelerateInterpolator(1.6f))
                    .start()
            }
            card.setOnClickListener { dismiss() }

            main.postDelayed({ dismiss() }, AUTO_DISMISS_MS)
            poll(app, mode)
            true
        }.getOrElse {
            GuardRail.error("OverlayGuide", "overlay window failed ($mode) — falling back", it)
            shown = null
            false
        }
    }

    /** Clears the card the moment the user satisfies the request underneath. */
    private fun poll(context: Context, mode: String) {
        main.postDelayed(object : Runnable {
            override fun run() {
                if (shown == null) return
                if (GuideSheetActivity.satisfied(context, mode)) dismiss() else main.postDelayed(this, POLL_MS)
            }
        }, POLL_MS)
    }

    fun dismiss() {
        val card = shown ?: return
        shown = null
        main.removeCallbacksAndMessages(null)
        runCatching {
            val windowManager = card.context.applicationContext
                .getSystemService(WindowManager::class.java)
            windowManager.removeView(card)
        }.onFailure { GuardRail.error("OverlayGuide", "overlay window removal failed", it) }
    }
}
