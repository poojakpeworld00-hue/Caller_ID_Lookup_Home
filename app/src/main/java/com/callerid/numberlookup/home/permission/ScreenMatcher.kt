package com.callerid.numberlookup.home.permission

/**
 * Decides which [AccessRule]s apply to a given Activity.
 *
 * Matching is done on the Activity's *simple class name*, case-insensitively,
 * so Remote Config stays readable and refactors that only move a class between
 * packages don't break targeting.
 *
 * [LEGACY_NAMES] maps every name an earlier build used to the current class names,
 * so Remote Config documents written against any of them keep matching. Two
 * renames have happened, and the table is chained: a first-generation name maps
 * straight to today's class, not to the intermediate one.
 *
 * The value is a list because one old name can now mean two screens: the original
 * app's single MainActivity became this app's AppHubActivity, and the launcher
 * module later brought its own MainActivity, which is now HomeDeckActivity. A rule
 * targeting "MainActivity" has always applied to both, and still does.
 *
 * Drop an entry once the server-side rule has been updated.
 */
object ScreenMatcher {

    private val LEGACY_NAMES: Map<String, List<String>> = mapOf(
        "ADDashboardActivity" to listOf("AdRelayActivity"),
        "ADHomeActivity" to listOf("AdRelayActivity"),
        "BaseActivity" to listOf("ScreenBaseActivity"),
        "BatteryActivity" to listOf("BatteryToolActivity"),
        "BearingActivity" to listOf("HeadingToolActivity"),
        "BlockRosterActivity" to listOf("BlockLedgerActivity"),
        "BlocklistActivity" to listOf("BlockLedgerActivity"),
        "CallDetailActivity" to listOf("CallBriefActivity"),
        "CallInsightActivity" to listOf("CallBriefActivity"),
        "ChronoActivity" to listOf("StopClockActivity"),
        "CompassActivity" to listOf("HeadingToolActivity"),
        "ConsentActivity" to listOf("AgreementActivity"),
        "CountdownActivity" to listOf("EggTimerActivity"),
        "CountryPickerActivity" to listOf("RegionPickerActivity"),
        "DecibelActivity" to listOf("NoiseToolActivity"),
        "DialerActivity" to listOf("DialPadActivity"),
        "FlashlightActivity" to listOf("FlashToolActivity"),
        "FloatAccessActivity" to listOf("OverlayGateActivity"),
        "FsiPermissionActivity" to listOf("FsiGateActivity"),
        "FullScreenAccessActivity" to listOf("FsiGateActivity"),
        "HiddenIconsActivity" to listOf("MaskedAppsActivity"),
        "HostActivity" to listOf("ScreenBaseActivity"),
        "IdentifyInsightActivity" to listOf("LookupBriefActivity"),
        "IdentifyTraceActivity" to listOf("LookupLogActivity"),
        "InboundCallActivity" to listOf("RingScreenActivity"),
        "IncomingCallActivity" to listOf("RingScreenActivity"),
        "IntroActivity" to listOf("TourActivity"),
        "KeypadActivity" to listOf("DialPadActivity"),
        "LanguageActivity" to listOf("LanguagePickActivity"),
        "LaunchActivity" to listOf("BootSplashActivity"),
        "LevelActivity" to listOf("LevelToolActivity"),
        "LightMeterActivity" to listOf("BrightToolActivity"),
        "LocaleActivity" to listOf("LanguagePickActivity"),
        "LookupDetailActivity" to listOf("LookupBriefActivity"),
        "LookupHistoryActivity" to listOf("LookupLogActivity"),
        "LuxMeterActivity" to listOf("BrightToolActivity"),
        "MainActivity" to listOf("AppHubActivity", "HomeDeckActivity"),
        "OnboardingActivity" to listOf("TourActivity"),
        "OnboardingDefaultLauncherActivity" to listOf("DefaultHomeStepActivity"),
        "OnboardingWelcomeActivity" to listOf("WelcomeStepActivity"),
        "OverlayGuideActivity" to listOf("GuideSheetActivity"),
        "OverlayPermissionActivity" to listOf("OverlayGateActivity"),
        "PowerGaugeActivity" to listOf("BatteryToolActivity"),
        "PreferencesActivity" to listOf("OptionsDeckActivity"),
        "SettingsActivity" to listOf("DeckSettingsActivity", "OptionsDeckActivity"),
        "ShellActivity" to listOf("AppHubActivity"),
        "SimCardActivity" to listOf("SimDeckActivity"),
        "SimInfoActivity" to listOf("SimDeckActivity"),
        "SimpleActivity" to listOf("CoreDeckActivity"),
        "SoundMeterActivity" to listOf("NoiseToolActivity"),
        "SpeedometerActivity" to listOf("SpeedToolActivity"),
        "SplashActivity" to listOf("BootSplashActivity"),
        "StopwatchActivity" to listOf("StopClockActivity"),
        "TermsActivity" to listOf("AgreementActivity"),
        "TerritoryPickerActivity" to listOf("RegionPickerActivity"),
        "TiltActivity" to listOf("LevelToolActivity"),
        "TimerActivity" to listOf("EggTimerActivity"),
        "ToolsActivity" to listOf("ToolboxActivity"),
        "TorchActivity" to listOf("FlashToolActivity"),
        "UtilityActivity" to listOf("ToolboxActivity"),
        "VelocityActivity" to listOf("SpeedToolActivity"),
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
            LEGACY_NAMES[configuredName]
                ?.any { it.equals(activitySimpleName, ignoreCase = true) } == true

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
