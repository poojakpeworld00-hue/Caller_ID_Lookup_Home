package com.callerid.numberlookup.home.services

import android.content.Context
import android.content.res.ColorStateList
import android.telephony.TelephonyManager
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.facebook.shimmer.ShimmerFrameLayout
import com.callerid.numberlookup.home.R
import com.callerid.numberlookup.home.services.RetrofitClient
import com.callerid.numberlookup.home.services.ServiceCredentials
import com.callerid.numberlookup.home.data.CallLogSource
import com.callerid.numberlookup.home.data.PeopleSource
import com.callerid.numberlookup.home.ui.lookup.DigitInfo
import com.callerid.numberlookup.home.ui.common.CallPresenter
import com.callerid.numberlookup.home.util.GuardRail

/**
 * Resolves caller details and renders them into [R.layout.part_caller_id].
 *
 * Shared by [com.callerid.numberlookup.home.services.onincomming.IdentFloatService] (floating window, device unlocked) and
 * RingScreenActivity (full screen, device locked) so the card looks and reads
 * identically in both states.
 */
object IdentCard {

    /** The bits we surface on the card; resolved off the main thread. */
    data class Info(
        val name: String?,
        val known: Boolean,
        val callCount: Int,
        val network: String?
    )

    /**
     * Blocking lookup — call from a background thread.
     * Combines the contact name, how many times this number appears in the call
     * log, and the SIM operator name.
     */
    fun resolve(context: Context, number: String): Info {
        val name = runCatching { PeopleSource(context).lookupNameByNumber(number) }.getOrNull()

        val callCount = runCatching {
            val target = digitsTail(number)
            CallLogSource(context).getCalls(limit = 2000)
                .count { digitsTail(it.number) == target }
        }.getOrDefault(0)

        val network = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.networkOperatorName?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return Info(name = name, known = !name.isNullOrBlank(), callCount = callCount, network = network)
    }

    /** Binds [number] + resolved [info] into an inflated overlay card [root]. */
    fun bind(context: Context, root: View, number: String, info: Info) {
        val displayName = info.name?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.incall_unknown)

        root.findViewById<TextView>(R.id.tvIncallAvatar).text =
            CallPresenter.initials(info.name, number)
        root.findViewById<TextView>(R.id.tvIncallName).text = displayName
        root.findViewById<TextView>(R.id.tvIncallNumber).text = number

        bindStatusPill(context, root.findViewById(R.id.tvIncallStatus), info.known)

        root.findViewById<TextView>(R.id.tvIncallWhen).text =
            context.getString(R.string.incall_now)
        root.findViewById<TextView>(R.id.tvIncallCalls).text =
            context.getString(R.string.incall_calls, info.callCount)
        root.findViewById<TextView>(R.id.tvIncallNetwork).text =
            info.network?.takeIf { it.isNotBlank() } ?: "—"
    }

    /**
     * The name the caller-ID network has for [number], or null when it has none.
     *
     * The same `similar-phone-number` endpoint the Lookup screen searches with, so a ringing
     * stranger is identified exactly as typing that number into search would identify them.
     *
     * Deliberately NOT folded into [resolve]: that one is local-only and finishes in
     * milliseconds, and the card has to be on screen while the phone is still ringing. This
     * runs after it and upgrades the name in place, so a slow or dead network costs the user
     * nothing — they just keep the local result.
     */
    suspend fun networkName(number: String): String? {
        if (!ServiceCredentials.isConfigured) return null
        val query = DigitInfo.normalize(number).takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val response = RetrofitClient.api.checkPhoneNumber(
                id = ServiceCredentials.API_ID,
                phone = query,
                hashKey = ServiceCredentials.API_HASH,
                token = ServiceCredentials.API_TOKEN,
            )
            if (!response.isSuccessful) {
                GuardRail.log(TAG, "lookup failed (${response.code()}) for $number")
                null
            } else {
                response.body()?.data.orEmpty()
                    .firstNotNullOfOrNull { it.name?.trim()?.takeIf(String::isNotBlank) }
            }
        }.onFailure { GuardRail.error(TAG, "lookup error for $number", it) }.getOrNull()
    }

    /**
     * Swaps the name line for a shimmering bar while [networkName] is in flight.
     *
     * The card is already up and saying "Unknown" at this point — the number is one the
     * device could not name — so without this the lookup is invisible and a name that lands a
     * second later reads as a glitch rather than as a result.
     */
    fun showNameLoading(root: View) {
        root.findViewById<TextView>(R.id.tvIncallName)?.visibility = View.GONE
        root.findViewById<ShimmerFrameLayout>(R.id.nameShimmer)?.apply {
            visibility = View.VISIBLE
            startShimmer()
        }
    }

    /**
     * Puts the name line back, whatever the lookup returned.
     *
     * Must run on every exit from the search — answer, no answer, error, or the call ending
     * mid-request — or the card is left shimmering over a name that is never coming.
     */
    fun hideNameLoading(root: View) {
        root.findViewById<ShimmerFrameLayout>(R.id.nameShimmer)?.apply {
            stopShimmer()
            visibility = View.GONE
        }
        root.findViewById<TextView>(R.id.tvIncallName)?.visibility = View.VISIBLE
    }

    /**
     * Replaces just the name, its initials and the status pill on an already-bound card.
     *
     * Used when [networkName] answers after [bind] has already put the local result on
     * screen — the rest of the card (call count, network, timing) does not change.
     */
    fun bindName(context: Context, root: View, number: String, name: String) {
        root.findViewById<TextView>(R.id.tvIncallAvatar).text = CallPresenter.initials(name, number)
        root.findViewById<TextView>(R.id.tvIncallName).text = name
        bindStatusPill(context, root.findViewById(R.id.tvIncallStatus), known = true)
    }

    private const val TAG = "IdentCard"

    /** Green "Known Contact" vs neutral "Unknown" pill. */
    private fun bindStatusPill(context: Context, pill: TextView, known: Boolean) {
        val textRes = if (known) R.string.incall_known else R.string.incall_unknown
        val fgRes = if (known) R.color.success else R.color.on_surface_variant
        val bgRes = if (known) R.color.success_soft else R.color.neutral_soft
        val iconRes = if (known) R.drawable.ic_verified else R.drawable.ic_info

        val fg = ContextCompat.getColor(context, fgRes)
        pill.setText(textRes)
        pill.setTextColor(fg)
        pill.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bgRes))
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(pill, ColorStateList.valueOf(fg))
    }

    /** Last 9 digits — tolerant comparison that ignores country code / formatting. */
    private fun digitsTail(number: String): String =
        number.filter { it.isDigit() }.takeLast(9)
}
