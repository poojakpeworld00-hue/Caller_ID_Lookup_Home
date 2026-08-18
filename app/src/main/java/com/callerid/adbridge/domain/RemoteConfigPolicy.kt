package com.callerid.adbridge.domain

import com.callerid.numberlookup.home.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * One place to decide how Remote Config fetches behave.
 *
 * [FirebaseRemoteConfig.setConfigSettingsAsync] applies to the singleton, so two callers with
 * different settings do not each get their own — the last one to run defines the behaviour for
 * everything after it. The splash asked for 1s and the permission engine for 3600s, which made
 * the effective interval a question of startup ordering rather than of intent.
 *
 * The values kept here are the splash's, since that is the call that decides how fresh the
 * config is at a cold start, and in practice it was already winning the race.
 *
 * With LiveConfigWatcher in place a change published while the app is running arrives by push
 * regardless of this interval. It still governs the cold-start case: a change published while
 * the app was closed is picked up by the next fetch, so raising the interval would delay
 * exactly that case.
 */
object RemoteConfigPolicy {

    /** Debug takes every publish immediately; release still fetches on each cold start. */
    private val MIN_FETCH_INTERVAL_SECONDS = if (BuildConfig.DEBUG) 0L else 1L

    /**
     * Caps the fetch so a slow network cannot park the splash on this call (it defaulted to
     * 60s; 37s stalls were observed). On timeout the fetch fails fast and the flow continues
     * on cached values.
     */
    private const val FETCH_TIMEOUT_SECONDS = 10L

    fun settings(): FirebaseRemoteConfigSettings =
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
            .setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS)
            .build()

    fun applyTo(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.setConfigSettingsAsync(settings())
    }
}
