package com.callerid.adbridge.presentation

import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.widget.TextView
import com.callerid.numberlookup.home.R
import com.callerid.numberlookup.home.launcher.extensions.isDefaultLauncher
import com.callerid.numberlookup.home.util.GuardRail


import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Transparent hint screen shown alongside the system "Appear on top" Settings
 * page. Sits in the caller's task and presents a bottom card pointing at the
 * toggle. Auto-finishes the moment overlay permission is granted, so the user
 * lands cleanly back on the caller without an extra tap.
 *
 * Tapping anywhere outside the card also dismisses the hint.
 */
class GuideSheetActivity : AppCompatActivity() {

    companion object {
        private const val AUTO_DISMISS_MS = 3_000L

        private const val EXTRA_MODE = "mode"

        /** System "display over other apps" list. */
        const val MODE_OVERLAY = "overlay"

        /** System "Default home app" list. */
        const val MODE_HOME = "home"

        /**
         * Stacks the guide on top of the system page the caller just opened.
         *
         * Best effort, exactly as [MODE_OVERLAY] callers have always used it: a guide
         * that fails to start must never take the Settings page down with it.
         */
        fun show(context: Context, mode: String) {
            runCatching {
                context.startActivity(
                    Intent(context, GuideSheetActivity::class.java).putExtra(EXTRA_MODE, mode)
                )
            }.onFailure { GuardRail.error("OverlayGuide", "guide failed to start ($mode)", it) }
        }
    }

    private val mode: String
        get() = intent?.getStringExtra(EXTRA_MODE) ?: MODE_OVERLAY

    /**
     * What the user has to do on the page underneath. Polled while we are on top, so
     * the guide clears itself the moment the switch flips or the home app changes.
     */
    private fun isSatisfied(): Boolean = when (mode) {
        MODE_HOME -> runCatching { isDefaultLauncher() }.getOrDefault(false)
        else -> Settings.canDrawOverlays(this)
    }

    private var pollJob: Job? = null
    private var autoDismissJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_overlay_guide)

        val root = findViewById<View>(R.id.llMain)

        if (mode == MODE_HOME) {
            // Same card, same timing — only the wording changes. The row shows the
            // launcher name because that is what the home-app list labels it with,
            // where the overlay list uses the longer overlay name.
            findViewById<TextView>(R.id.guideTitleTv)?.setText(R.string.home_guide_title)
            findViewById<TextView>(R.id.guideDescTv)?.setText(R.string.home_guide_desc)
            findViewById<TextView>(R.id.guideRowNameTv)?.setText(R.string.app_name)
            findViewById<TextView>(R.id.guideRowHintTv)?.setText(R.string.home_guide_row_hint)
        }

        // Edge-to-edge is forced on Android 15+/16 (targetSdk 37), so the bottom
        // hint card would otherwise draw behind the navigation bar. Pad the root
        // by the system-bar insets so the card floats above the nav bar (and
        // clears side/gesture insets in landscape).
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        // Translucent windows can miss the initial inset pass — force one.
        ViewCompat.requestApplyInsets(root)

        root?.setOnClickListener {
            finish()
        }

        // Slide the card up on entry. The window itself is translucent and the dim
        // fades in on its own, so animating the card is what makes it read as a
        // sheet rising over the Settings page rather than a frame-one pop-in.
        findViewById<View>(R.id.overlayCardVw)?.apply {
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

        // Auto-dismiss after 3s. Lives on lifecycleScope so it cancels on
        // destroy, and runs once per activity instance — pausing (e.g. user
        // pulled down the notification shade) does not reset the timer.
        autoDismissJob = lifecycleScope.launch {
            delay(AUTO_DISMISS_MS)
            if (!isFinishing && !isDestroyed) finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // If the user satisfied the request while we were paused (because Settings
        // was on top), close ourselves so the caller's UI is fully visible.
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                if (isSatisfied()) {
                    finish()
                    return@launch
                }
                delay(500L)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pollJob?.cancel()
    }
}
