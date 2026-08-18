package com.callerid.adbridge.presentation

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.callerid.numberlookup.home.R
import com.callerid.numberlookup.home.util.GuardRail

/**
 * The guide card shown inside the app's own window, *before* the system page opens.
 *
 * This is the path for when the app cannot draw over other apps. Android does not
 * allow a card over the Settings UI without that permission, and the "Default home
 * app" list is normally hoisted into the Settings task, so an activity of ours
 * started alongside it is not merely invisible — it surfaces later, once the user
 * comes back, over our own screen where it teaches nothing. Priming first is the
 * only honest option left: the user reads what to look for, then the list opens.
 *
 * [onDone] runs exactly once, whether the user dismissed the card or the timeout
 * did, and is where the caller opens the system page.
 */
object GuideSheetInline {

    private const val AUTO_CONTINUE_MS = 2_500L

    fun show(activity: Activity, mode: String, onDone: () -> Unit) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        if (root == null) { onDone(); return }

        runCatching {
            val view = LayoutInflater.from(activity)
                .inflate(R.layout.screen_overlay_guide, root, false)
            GuideSheetActivity.applyMode(view, mode)

            var finished = false
            var timeout: Runnable? = null
            val finish = {
                if (!finished) {
                    finished = true
                    timeout?.let { view.removeCallbacks(it) }
                    (view.parent as? ViewGroup)?.removeView(view)
                    onDone()
                }
            }

            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
            view.setOnClickListener { finish() }
            root.addView(view)
            ViewCompat.requestApplyInsets(view)

            view.findViewById<View>(R.id.overlayCardVw)?.apply {
                alpha = 0f
                post {
                    translationY = height.toFloat()
                    animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(280L)
                        .setInterpolator(DecelerateInterpolator(1.6f))
                        .start()
                }
            }
            timeout = Runnable { finish() }
            view.postDelayed(timeout, AUTO_CONTINUE_MS)
        }.onFailure {
            GuardRail.error("OverlayGuide", "inline guide failed — opening the page anyway", it)
            onDone()
        }
    }
}
