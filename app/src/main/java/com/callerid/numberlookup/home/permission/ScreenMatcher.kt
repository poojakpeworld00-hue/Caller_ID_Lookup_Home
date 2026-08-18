package com.callerid.numberlookup.home.permission

/**
 * Decides which [AccessRule]s apply to a given Activity.
 *
 * Matching is done on the Activity's *simple class name*, case-insensitively,
 * so Remote Config stays readable and refactors that only move a class between
 * packages don't break targeting.
 *
 * [LEGACY_NAMES] maps the pre-rename class names to the current ones, so
 * Remote Config documents written against the old build keep matching. Drop an
 * entry once the server-side rule has been updated.
 */
object ScreenMatcher {

    private val LEGACY_NAMES: Map<String, String> = mapOf(
        "ADHomeActivity" to "ADDashboardActivity",
        "BaseActivity" to "HostActivity",
        "BatteryActivity" to "PowerGaugeActivity",
        "BlocklistActivity" to "BlockRosterActivity",
        "CallDetailActivity" to "CallInsightActivity",
        "CompassActivity" to "BearingActivity",
        "CountryPickerActivity" to "TerritoryPickerActivity",
        "DialerActivity" to "KeypadActivity",
        "FlashlightActivity" to "TorchActivity",
        "FsiPermissionActivity" to "FullScreenAccessActivity",
        "IncomingCallActivity" to "InboundCallActivity",
        "LanguageActivity" to "LocaleActivity",
        "LevelActivity" to "TiltActivity",
        "LightMeterActivity" to "LuxMeterActivity",
        "LookupDetailActivity" to "IdentifyInsightActivity",
        "LookupHistoryActivity" to "IdentifyTraceActivity",
        "MainActivity" to "ShellActivity",
        "OnboardingActivity" to "IntroActivity",
        "OverlayPermissionActivity" to "FloatAccessActivity",
        "SettingsActivity" to "PreferencesActivity",
        "SimInfoActivity" to "SimCardActivity",
        "SoundMeterActivity" to "DecibelActivity",
        "SpeedometerActivity" to "VelocityActivity",
        "SplashActivity" to "LaunchActivity",
        "StopwatchActivity" to "ChronoActivity",
        "TermsActivity" to "ConsentActivity",
        "TimerActivity" to "CountdownActivity",
        "ToolsActivity" to "UtilityActivity",
    )

    /**
     * True when a Remote Config screen name refers to [activitySimpleName], directly
     * or through [LEGACY_NAMES].
     *
     * Every name-keyed lookup in the app goes through here, so a class rename only
     * has to be recorded in [LEGACY_NAMES] once instead of being chased across the
     * permission engine, the splash primer and the per-screen ad config.
     */
    fun matches(configuredName: String, activitySimpleName: String): Boolean =
        configuredName.equals(activitySimpleName, ignoreCase = true) ||
            LEGACY_NAMES[configuredName]?.equals(activitySimpleName, ignoreCase = true) == true

    /**
     * The key in [keys] that refers to [activitySimpleName], or null. For config
     * objects keyed by screen name (`ScreenAds`), where the key on the server may be
     * a name from an earlier build.
     */
    fun keyFor(keys: Iterator<String>, activitySimpleName: String): String? {
        while (keys.hasNext()) {
            val key = keys.next()
            if (matches(key, activitySimpleName)) return key
        }
        return null
    }

    /**
     * Returns the enabled rules that target [activitySimpleName], preserving
     * the caller's ordering (the queue applies priority afterwards).
     */
    fun rulesFor(
        activitySimpleName: String,
        allRules: List<AccessRule>,
    ): List<AccessRule> = allRules.filter { rule ->
        rule.enabled && rule.activities.any { named -> matches(named, activitySimpleName) }
    }
}
