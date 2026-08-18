package com.callerid.adbridge.domain

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.callerid.adbridge.presentation.CustomAdsRegistry
import com.callerid.numberlookup.home.BuildConfig
import com.callerid.numberlookup.home.util.AppVault
import com.callerid.numberlookup.home.util.AppVault.THEME_DARK
import com.callerid.numberlookup.home.util.AppVault.THEME_LIGHT
import com.callerid.numberlookup.home.util.AppVault.THEME_SYSTEM
import org.json.JSONObject

/**
 * Reads a getData blob into [AdsVault].
 *
 * Lifted out of AdRelayActivity so it is not tied to the splash: the same ingest has to run
 * when Remote Config pushes a change to a running app (see LiveConfigWatcher), and duplicating
 * it would leave two lists of keys to keep in step.
 *
 * Facebook SDK initialisation stays with the caller — it needs an Activity and only makes
 * sense once per process — so [ingest] hands the credentials back instead of applying them.
 */
object AdConfigIngest {

    private const val CONFIG_TAG = "AdConfig"

    /** The Facebook credentials found in the blob; empty when the blob carries none. */
    data class FacebookKeys(val appId: String, val clientToken: String) {
        val usable: Boolean get() = appId.isNotEmpty() && clientToken.isNotEmpty()
    }

    /**
     * Reads every getData key from [root] into AdsVault (batched). [root] is
     * either the flat response or one of its `marketing` / `organic` sub-objects
     * (see [audienceRoot]). Safe to call again (funOnAdsLoad re-applies the correct
     * audience once the referrer settles OnMaketing).
     */
    fun ingest(context: Context, root: JSONObject): FacebookKeys {
        val adsPref = AdsVault.getInstance(context)
        adsPref.update {
            // --- Booleans ---
            listOf(
                "IsAdsON", "IsFail_FB", "isLoaderForFB", "IsCustomADS", "IsBack",
                "NativeBanner", "BannerAds", "In_App_Update_Show", "In_App_Update_Force_Show",
                "Iscountry_Counter", "Iscountry_Marketing_Counter", "HD_VBC_Show",
                "HD_VBC_Native", "is_preload_ads",
                "is_splash_inter_show", "is_splash_ads", "InterAds", "AppopenAds",
                "NativeAd", "is_rateus", "is_share", "Perm_Sheet_Show",
                "screen_wise_ad", "screen_wise_default"
            ).forEach { key -> if (root.has(key)) putBoolean(key, root.optBoolean(key, false)) }

            // --- Strings ---
            listOf(
                "IsAdType", "In_App_Update_Link", "CountryList_Counter_NShow",
                "CountryList_Marketing_Counter_NShow", "PrivacyPolicy", "TermLink",
                "DirectLink", "MarketLink", "HD_VBC_Native_ID", "HD_VBC_Banner_ID",
                "googleS_Inter", "googleBackInter", "googleInter", "googleAppopen",
                "googleNative", "googleBanner", "googleRewarded", "faceB_InterAds",
                "faceB_NativeAds", "faceB_NativeBannerAds", "faceB_BannerAds",
                "NativeTheme", "HD_VBC_Type", "NativeBgColor", "NativebtnColor",
                "NativetxtColor", "NativebtntxtColor", "Perm_Sheet_Mode",
                // API origin — see RetrofitClient, which falls back to its compiled-in default
                // when this is absent or malformed.
                "api_base_url",
                // Nested JSON objects stored as text (read back via JSONObject).
                "intro_display", "ScreenAds", "launcher_ads"
            ).forEach { key -> if (root.has(key)) putString(key, root.optString(key, "")) }

            // --- Integers ---
            listOf(
                "InterCounter", "InterBackCounter", "MarketInterCounter", "MarketBackCounter",
                "NativeCounter", "MarketNativeCounter", "MidNativeCounter", "BannerCounter",
                "MarketBannerCounter", "MarketAppopenCounter", "AppopenCounter",
                "Perm_Sheet_Interval_Days", "HD_VBC_Hrs"
            ).forEach { key -> if (root.has(key)) putInt(key, root.optInt(key, 0)) }

            applyNativeTheme(context, root) // DEFAULT theme

            // --- Custom Ads ---
            val customAdsArray = root.optJSONArray("custom_ads")
            if (customAdsArray != null) {
                putString("CUSTOM_ADS", customAdsArray.toString())
                CustomAdsRegistry.clearCache()
            }
        }

        // Facebook Ad initialization parameters
        val fbAppId = root.optString("FbAppId", "")
        val fbClientToken = root.optString("FbClientToken", "")

        if (BuildConfig.DEBUG) Log.d(
            CONFIG_TAG,
            "ingested → IsAdsON=${adsPref.getBoolean("IsAdsON")}, IsAdType=${adsPref.getString("IsAdType")}, " +
                "InterAds=${adsPref.getBoolean("InterAds")}, AppopenAds=${adsPref.getBoolean("AppopenAds")}, " +
                "NativeAd=${adsPref.getBoolean("NativeAd")}, BannerAds=${adsPref.getBoolean("BannerPromo")}, " +
                "HD_VBC_Show=${adsPref.getBoolean("HD_VBC_Show")}, HD_VBC_Hrs=${adsPref.getInt("HD_VBC_Hrs")}, " +
                "screen_wise_ad=${adsPref.getBoolean("screen_wise_ad")}, " +
                "customAds=${root.optJSONArray("custom_ads")?.length() ?: 0}, " +
                "fbInit=${fbAppId.isNotEmpty() && fbClientToken.isNotEmpty()}, " +
                "appOpenId=${adsPref.getString("googleAppopen")}"
        )

        return FacebookKeys(fbAppId, fbClientToken)
    }

    /**
     * The audience-specific sub-object of a getData response — `marketing` or
     * `organic` per [isMarketing], falling back to the other audience, then to the
     * flat [response] itself (legacy, un-split config → unchanged behaviour).
     */
    fun audienceRoot(response: JSONObject, isMarketing: Boolean): JSONObject {
        val preferred = if (isMarketing) "marketing" else "organic"
        val fallback = if (isMarketing) "organic" else "marketing"
        response.optJSONObject(preferred)?.let {
            if (BuildConfig.DEBUG) Log.d(CONFIG_TAG, "audienceRoot → using '$preferred' segment")
            return it
        }
        response.optJSONObject(fallback)?.let {
            if (BuildConfig.DEBUG) Log.d(CONFIG_TAG, "audienceRoot → '$preferred' missing, fell back to '$fallback' segment")
            return it
        }
        if (BuildConfig.DEBUG) Log.d(CONFIG_TAG, "audienceRoot → no marketing/organic wrapper, using flat config")
        return response
    }

    fun nativeThemeKey(context: Context): String {

        return when (AppVault.selectedTheme(context)) {
            THEME_DARK -> {
                "NativeDark"
            }

            THEME_LIGHT -> {
                "NativeLight"
            }

            THEME_SYSTEM -> {
                val isSystemDark =
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isSystemDark) "NativeDark" else "NativeLight"
            }

            else -> {
                "NativeLight"
            }
        }
    }

    fun applyNativeTheme(
        context: Context, response: JSONObject
    ) {
        val adsPreference = AdsVault.getInstance(context)

        val nativeThemeRoot = response.optJSONObject("NativeTheme") ?: return

        val modeKey = nativeThemeKey(context)

        nativeThemeRoot?.let {
            val marketingObj = it.optJSONObject("marketing")
            val defaultObj = it.optJSONObject("default")

            // Convert JSONObjects to strings before storing in AdsVault
            val marketingStr = marketingObj?.toString() ?: "{}"
            val defaultStr = defaultObj?.toString() ?: "{}"

            adsPreference.putString("NativeTheme_marketing", marketingStr)
            adsPreference.putString("NativeTheme_default", defaultStr)

            val themeJson = defaultObj?.optJSONObject(modeKey)

            if (themeJson != null) {
                adsPreference.putString("NativebtnColor", themeJson.optString("btnColor"))
                adsPreference.putString("NativebtntxtColor", themeJson.optString("btnText"))
                adsPreference.putString("NativeBgColor", themeJson.optString("bgColor"))
                adsPreference.putString("NativetxtColor", themeJson.optString("textColor"))
            }
        }
    }
}
