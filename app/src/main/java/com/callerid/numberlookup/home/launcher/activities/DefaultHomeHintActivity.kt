package com.callerid.numberlookup.home.launcher.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.callerid.adbridge.presentation.GuideSheetActivity
import com.callerid.numberlookup.home.R
import com.callerid.numberlookup.home.util.GuardRail

/**
 * The hint shown over the system "Default home app" list, pointing at the row to pick.
 *
 * ### Why an activity, not an overlay
 *
 * A `WindowManager` overlay needs `SYSTEM_ALERT_WINDOW`, which this app only collects later
 * in the flow — so an overlay version is silently never shown on a first run: the list opens
 * normally and the hint never appears. An activity needs no permission at all.
 *
 * ### Why it can be shown over another app at all
 *
 * [com.callerid.numberlookup.home.launcher.helpers.LauncherHintPrompt] starts it only once
 * Settings is genuinely in front, inside the brief background-start grace an app keeps after
 * being in the foreground. `NEW_TASK` plus the manifest's empty `taskAffinity` put it in a
 * task of its own — with the default affinity it would land in *our* task and drag the whole
 * app forward, hiding the very screen it is annotating.
 *
 * ### It gates the screen underneath, briefly
 *
 * A translucent activity is a real focused window, so the list behind it is paused while this
 * is up. `FLAG_NOT_FOCUSABLE` hands key input back, and the first touch anywhere dismisses it,
 * so choosing a launcher costs one extra tap only while the card is on screen — and none
 * after [AUTO_FINISH_MS].
 */
class DefaultHomeHintActivity : AppCompatActivity() {

    private val autoFinish = Runnable { if (!isFinishing) finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_overlay_guide)
        GuardRail.log(TAG, "hint shown over the home-app list")

        // NOT_FOCUSABLE keeps key input with the settings screen and implies NOT_TOUCH_MODAL.
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        val root = findViewById<android.view.View>(R.id.llMain)
        GuideSheetActivity.applyMode(root, GuideSheetActivity.MODE_HOME)

        // targetSdk 36 is edge-to-edge with no opt-out, so this window spans the bars and the
        // card would otherwise sit under the navigation bar. Insets are returned unchanged —
        // nothing below this consumes them, but the list behind still gets its own dispatch.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // The label and icon the home-app list itself shows, rather than the app_name string,
        // so the card names exactly the row the user is hunting for.
        runCatching {
            findViewById<ImageView>(R.id.guideRowIconIv)
                ?.setImageDrawable(packageManager.getApplicationIcon(applicationInfo))
            findViewById<TextView>(R.id.guideRowNameTv)?.text =
                packageManager.getApplicationLabel(applicationInfo)
        }

        bindDismissOnTouch(root)

        visible = this
        root.postDelayed(autoFinish, AUTO_FINISH_MS)
    }

    /**
     * Any touch takes the hint down, not just a dismiss button.
     *
     * The card covers a list the user is trying to use; making them find a specific target
     * first would be the one thing more annoying than the card. Returning true consumes the
     * gesture, so the tap that dismisses does not also land on a radio button — the launcher
     * gets chosen deliberately on the next tap, not by accident on this one.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bindDismissOnTouch(root: android.view.View) {
        root.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && !isFinishing) finish()
            true
        }
    }

    override fun onDestroy() {
        findViewById<android.view.View>(R.id.llMain)?.removeCallbacks(autoFinish)
        if (visible === this) visible = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DefaultHomeHint"

        /** Long enough to read the card, short enough to stay out of the way. */
        private const val AUTO_FINISH_MS = 3_000L

        /**
         * The live instance, so the prompt can take the hint down the moment the user is
         * back. Cleared in [onDestroy], so it never outlives the activity.
         */
        private var visible: DefaultHomeHintActivity? = null

        /**
         * `NEW_TASK` is required to start this from the background at all; the empty
         * `taskAffinity` in the manifest is what keeps that task from being our own.
         */
        fun intent(context: Context): Intent =
            Intent(context, DefaultHomeHintActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** No-op when nothing is showing. */
        fun dismiss() {
            visible?.takeUnless { it.isFinishing }?.finish()
            visible = null
        }
    }
}
