# Clone inputs — edit the "New value" column and tell me to apply

Source: `../CallerIDNumberLookupBlock` @ `d4c3ed1` (`com.calleridapp.numberlookup`, versionCode 4 / 1.0.3)
Clone:  this folder, branch **master**

Legend:
- **[SET]** — already applied. Change it only if you want something different.
- **[NEEDS YOU]** — still the source app's value, or a placeholder. The app builds, but this must be real before release.
- **[KEPT]** — deliberately unchanged. Reason given.

---

## A. Identity — done in Stage 2 (`d3adc1f`)

| # | Item | Source | Current value | New value |
|---|---|---|---|---|
| A1 | applicationId **[SET]** | `com.calleridapp.numberlookup` | `com.callerid.numberlookup.home` | |
| A2 | namespace **[SET]** | same as A1 | `com.callerid.numberlookup.home` | |
| A3 | Ad-module package **[SET]** | `com.calleridapp.admesh` | `com.callerid.adbridge` | |
| A4 | rootProject.name **[SET]** | `Caller ID Number Lookup Block` | `Caller ID Lookup Home` | |
| A5 | APK archive prefix **[SET]** | `CallerIdNumberLookupBlock` | `CallerIdLookupHome` | |
| A6 | Theme name **[SET]** | `Theme.CallerLookupBlock` | `Theme.CallerIdLookupHome` | |
| A7 | versionCode **[SET]** | 4 | 1 | |
| A8 | versionName **[SET]** | 1.0.3 | 1.0.0 | |
| A9 | minSdk / targetSdk **[KEPT]** | 26 / 36 | 26 / 36 | |

> A9: minSdk is 26, not 24 — the launcher module needs `LauncherApps` shortcut APIs.

## B. Names shown to users

| # | Item | Source | Current value | New value |
|---|---|---|---|---|
| B1 | `app_name` **[SET]** | `Caller ID: Number Lookup & Block` | `Caller ID Home` | |
| B2 | `app_label` (launcher) **[SET]** | localised `Caller ID: …` per locale | `␣␣Caller ID Home` | |
| B3 | `app_name_overlay` **[SET]** | `Caller ID Lookup & Block` | `Caller ID Lookup Home` | |
| B4 | Locale coverage **[SET]** | — | B1/B2 applied to all 12 `values*/strings.xml` (default + ar es fr hi ja pt ru th tr vi zh) | |

> The manifest labels the application with `app_label`, not `app_name` — Stage 2
> updated only `app_name`, so the launcher icon still read the source app's brand
> in all 12 locales until this pass.
>
> B2 keeps the two leading non-breaking spaces (`&#160;&#160;`) that sort the app to
> the top of system lists, and now applies them in every locale — the localised
> labels used a plain leading space, which AAPT trims, so the trick only worked in
> English. The brand itself is no longer translated, matching how `app_name` is
> handled.

## C. Brand visuals — **[NEEDS YOU]**, nothing changed yet

| # | Item | Where | State |
|---|---|---|---|
| C1 | `values/colors.xml` | whole palette | **byte-identical to the source app** — `primary` `#2C6547`, `primary_dark` `#1C4730` |
| C2 | Splash gradient | `splash_grad_start/center/end` | identical — `#047439 → #15A73D → #37C747` |
| C3 | Launcher icon | `mipmap-xhdpi/ic_launcher.png`, `drawable/ic_launcher_foreground.xml`, `…_background.xml` | all three byte-identical to the source app |
| C4 | Layouts | `res/layout/*` | only differ from the source by the package/theme rename |
| C5 | Verdict colours **[KEPT]** | `values/colors.xml` | green/red/amber are pinned to meaning ("identified"/"spam"/"suspicious"), not to the brand |

> Two apps on Play with the same icon, palette and splash read as one app
> submitted twice. This is the main body of work left.

## D. Keys and endpoints

| # | Item | Where | Value | Status |
|---|---|---|---|---|
| D1 | Backend base URL | `services/RetrofitClient.kt:25` | `https://callerid.kpeworld.com/` (Remote Config can override) | **[KEPT]** your own backend |
| D2 | API account id | `services/ServiceCredentials.kt` | `1433` | **[KEPT]** |
| D3 | API hash key | `services/ServiceCredentials.kt` | (28 chars) | **[KEPT]** |
| D4 | API bearer token | `services/ServiceCredentials.kt` | (132-char JWT) | **[KEPT]** |
| D5 | LightHouse API key | `local.properties` | `sk_a7u94m4mu6gcsvey7ydz2` | **[SET]** new key, distinct from the source app |
| D6 | LightHouse base URL | `local.properties` | `https://api.falconpush.com` | **[SET]** same endpoint as source |
| D7 | `google-services.json` | `app/` | project `caller-id-home` (`752107855402`), package matches A1 | **[SET]** |
| D8 | Remote Config template | `docs/remote-config.json` | not published to `caller-id-home` yet; line 326 `PrivacyPolicy` still points at `identifycaller.phonelookup.contacts.calllog` | **[NEEDS YOU]** |
| D9 | AdMob app id | `AndroidManifest.xml:405` | `ca-app-pub-3940256099942544~3347511713` — Google's **test** id | **[NEEDS YOU]** |
| D10 | Ad unit ids | `docs/remote-config.json` | every unit is a Google **test** unit | **[NEEDS YOU]** |
| D11 | Signing keystore | `certificate/calleridnumber.jks` | byte-identical to the source app's key; no `signingConfigs` block in Gradle, release signing is done from the IDE | **[NEEDS YOU]** own key for a separate listing |

> D5: `local.properties` is gitignored and had lost both LightHouse lines in the
> clone, so the SDK was initialising with an empty key
> (`app/build.gradle.kts:16-17` default to `""`, then XOR-scramble into
> `BuildConfig.LH_API_KEY`, read at `LookupShellApp.kt:70`). Push was silently dead.
>
> D1–D4 are your own backend, so the clone keeps using them. The
> `ServiceCredentials.isConfigured` guard stays in place; it exists so a build with
> placeholders can never fire those calls — notably `PersonUploader`, which uploads
> the user's contacts.

## E. Deliberately left alone — **[KEPT]**

| # | Item | Where | Why |
|---|---|---|---|
| E1 | `conduit.user` / `conduit.password` | `gradle.properties` | Private-maven read credentials — the build cannot resolve the LightHouse SDK without them. |
| E2 | Ad-SDK native layouts (9) | `res/layout/google*native*.xml`, `fb*native*.xml` | AdMob/FAN bind these views by reference; renaming their ids breaks ad rendering. |
| E3 | Kotlin class/file names (213) | `app/src/main/java/…` | Stage 2 renamed packages only. Identical to the source app file-for-file. |

---

## Still outstanding

1. **Brand visuals** (section C) — icon, palette, splash. Largest remaining item.
2. **Publish Remote Config** to `caller-id-home` from `docs/remote-config.json`, after fixing the stale `PrivacyPolicy` URL (D8). Without it, the ads and permission engines get no config.
3. **Real AdMob app id + ad units** (D9, D10).
4. **Own signing keystore** (D11).
5. Optional: **class/file-level rename pass** (E3) if the two apps should not share a code fingerprint.
