package com.callerid.adbridge.domain

import android.content.Context
import com.callerid.numberlookup.home.BuildConfig
import com.callerid.numberlookup.home.permission.AccessSource
import com.callerid.numberlookup.home.util.GuardRail
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import org.json.JSONObject

/**
 * Applies Remote Config changes while the app is running.
 *
 * Without this the blob is only read at splash, so a value published in the console reaches a
 * device on its next cold start — which for a launcher can be days, since the home screen is
 * rarely killed.
 *
 * Realtime Remote Config pushes the change instead: [ConfigUpdateListener.onUpdate] fires,
 * the new values are activated, and the same ingest the splash runs re-populates AdsVault, so
 * every gate that reads from it — ad slots, the permission engine, the settings rows — picks
 * the change up on its next read.
 *
 * The install-referrer step is deliberately not re-run: the audience a user landed in does not
 * change because a config value did, and re-running it would re-POST attribution.
 */
object LiveConfigWatcher {

    private const val TAG = "LiveConfig"

    private var registration: com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration? = null

    /** Idempotent: a second call replaces the previous registration rather than stacking one. */
    fun start(context: Context) {
        val app = context.applicationContext
        stop()

        registration = runCatching {
            FirebaseRemoteConfig.getInstance().addOnConfigUpdateListener(
                object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate: ConfigUpdate) {
                        GuardRail.log(TAG, "config update: ${configUpdate.updatedKeys}")
                        FirebaseRemoteConfig.getInstance().activate()
                            .addOnCompleteListener { apply(app) }
                    }

                    override fun onError(error: FirebaseRemoteConfigException) {
                        // Not fatal: the splash fetch still applies the change on next launch.
                        GuardRail.error(TAG, "realtime updates unavailable", error)
                    }
                }
            )
        }.onFailure { GuardRail.error(TAG, "could not register for config updates", it) }
            .getOrNull()
    }

    fun stop() {
        runCatching { registration?.remove() }
        registration = null
    }

    private fun apply(context: Context) {
        val blobKey = if (BuildConfig.DEBUG) "DEBUG_GET_DATA_LIST" else "GET_DATA_LIST"
        val raw = FirebaseRemoteConfig.getInstance().getString(blobKey)
        if (raw.isBlank()) {
            GuardRail.log(TAG, "$blobKey empty after update — keeping the values already in use")
            return
        }

        runCatching {
            val response = JSONObject(raw)
            val vault = AdsVault.getInstance(context)
            val onMarketing = vault.getBoolean("OnMaketing")

            vault.putString("GET_DATA_RAW", raw)
            vault.putBoolean(
                "__cfg_audience_split",
                response.has("marketing") || response.has("organic"),
            )
            AdConfigIngest.ingest(
                context,
                AdConfigIngest.audienceRoot(response, onMarketing),
            )

            // The permission engine caches its own parsed copy of the same blob.
            AccessSource.reload()
            GuardRail.log(TAG, "applied live config (marketing=$onMarketing)")
        }.onFailure { GuardRail.error(TAG, "live config could not be applied", it) }
    }
}
