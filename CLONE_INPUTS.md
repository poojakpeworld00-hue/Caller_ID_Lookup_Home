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

## C. Brand visuals — **[SET]** in Stage 3, from `ICON_3.png`

| # | Item | Where | State |
|---|---|---|---|
| C1 | Brand ramp | `values/colors_cid.xml`, `values-night/colors_cid.xml` | rebuilt on the icon's blue: `cid_g_700` `#0B5ED7`, ink `#0A2647`, container `#E1EDFF` |
| C2 | `primary` / `primary_dark` | `values/colors.xml` | `#0B5ED7` / `#0842A0` (night `#7FB2EE` / `#5E8CC4`) |
| C3 | Splash gradient | `splash_grad_start/center/end` | `#046DFF → #0A95FF → #0BD1FF`, sampled from the icon; dark→light direction kept from the source so white splash text keeps the darker end |
| C4 | Launcher icon | `mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` + `mipmap-anydpi-v26/ic_launcher.xml` | new art at 5 densities, plus a real adaptive icon (gradient background vector + white art foreground in the 66/108 safe zone) |
| C5 | Layouts | `res/layout/*` | still differ from the source only by the package/theme rename |
| C6 | Verdict colours **[KEPT]** | `values/colors.xml` | green/red/amber are pinned to meaning ("identified"/"spam"/"suspicious"); `online_green` / `whatsapp_green` name real products |

> 113 tokens were rotated off the green ramp. Lightness and alpha were preserved
> per token, so the design system's existing contrast relationships survive:
> white on `primary` 5.84, `on_primary_container` on `primary_container` 9.51,
> body ink on background 13.61 — all above the 4.5 AA floor.
>
> The token names still read `cid_g_*` ("g" for the old green ramp). Renaming
> them would touch every layout that references them; the values are blue, the
> names are historical.

## D. Keys and endpoints

| # | Item | Where | Value | Status |
|---|---|---|---|---|
| D1 | Backend base URL | `services/RetrofitClient.kt:25` | `https://callerid.kpeworld.com/` (Remote Config can override) | **[KEPT]** your own backend |
| D2 | API account id | `services/ServiceCredentials.kt` | `1433` | **[KEPT]** |
| D3 | API hash key | `services/ServiceCredentials.kt` | (28 chars) | **[KEPT]** |
| D4 | API bearer token | `services/ServiceCredentials.kt` | (132-char JWT) | **[KEPT]** |
| D5 | LightHouse API key | `local.properties` | `sk_a7u9…` (full value in `local.properties`, untracked) | **[SET]** new key, distinct from the source app |
| D6 | LightHouse base URL | `local.properties` | `https://api.falconpush.com/` | **[SET]** endpoint per the dashboard config |
| D7 | `google-services.json` | `app/` | project `caller-id-home` (`752107855402`), package matches A1 | **[SET]** |
| D8 | Remote Config value | `docs/remote-config.json` | content correct; **not published** to `caller-id-home` | **[NEEDS YOU]** |
| D8a | LightHouse dashboard | server-side | app entry needs the `caller-id-home` **service-account JSON** so its backend can send FCM v1 | **[NEEDS YOU]** |
| D8b | Policy / terms URLs | `strings.xml:474-475`, `docs/remote-config.json`, LightHouse disclosure | all three aligned on `sites.google.com/view/calleridphonelookup/{privacy,terms}` — the **original** app's site | **[SET]**, but see note |
| D9 | AdMob app id | `AndroidManifest.xml:405` | `ca-app-pub-3940256099942544~3347511713` — Google's **test** id | **[NEEDS YOU]** |
| D10 | Ad unit ids | `docs/remote-config.json` | every unit is a Google **test** unit | **[NEEDS YOU]** |
| D11 | Signing keystore | `certificate/calleridnumber.jks` | byte-identical to the source app's key; no `signingConfigs` block in Gradle, release signing is done from the IDE | **[NEEDS YOU]** own key for a separate listing |

> D5: `local.properties` is gitignored and had lost both LightHouse lines in the
> clone, so the SDK was initialising with an empty key
> (`app/build.gradle.kts:16-17` default to `""`, then XOR-scramble into
> `BuildConfig.LH_API_KEY`, read at `LookupShellApp.kt:70`). Push was silently dead.
>
> D8: `docs/remote-config.json` is not a Firebase template export — it is the
> *value* of one string parameter. `ADDashboardActivity.setResponceInPref` reads
> `GET_DATA_LIST` (release) / `DEBUG_GET_DATA_LIST` (debug) and picks the
> `marketing` or `organic` root by install referrer. `permission_engine`
> (`AccessSource.kt:96`) and `launcher_ads` are nested inside that blob, so no
> separate parameters are needed — just those two, both set to the same JSON.
>
> D8b: a policy page naming a different app is a common Play rejection trigger
> for a separate listing.
>
> D1–D4 are your own backend, so the clone keeps using them. The
> `ServiceCredentials.isConfigured` guard stays in place; it exists so a build with
> placeholders can never fire those calls — notably `PersonUploader`, which uploads
> the user's contacts.

## E. Deliberately left alone — **[KEPT]**

> Stage 3b/3c renamed all 35 Activity classes and 115 of 128 layouts (prefix swap:
> `activity_`→`screen_`, `fragment_`/`*_fragment`→`pane_`, `item_`→`cell_`,
> `dialog_`→`sheet_`, `view_`→`part_`, `widget_`→`gadget_`). Remote Config keeps
> matching through `ScreenMatcher.LEGACY_NAMES`, now chained across both renames.

| # | Item | Where | Why |
|---|---|---|---|
| E1 | `conduit.user` / `conduit.password` | `gradle.properties` | Private-maven read credentials — the build cannot resolve the LightHouse SDK without them. |
| E2 | Ad-SDK native layouts (9) | `res/layout/google*native*.xml`, `fb*native*.xml` | AdMob/FAN bind these views by reference; renaming their ids breaks ad rendering. |
| E3 | Fossify `BaseSimpleActivity` | `org.fossify.commons` | External library API — the local `SimpleActivity` subclass was renamed, its superclass cannot be. |

---

## Still outstanding

1. **Publish Remote Config** — `GET_DATA_LIST` + `DEBUG_GET_DATA_LIST` on `caller-id-home` (D8). Without it the ads and permission engines get no config at all.
2. **Real AdMob app id + ad units** (D9, D10) — and the `980.mark.qureka.com` `DirectLink`/`MarketLink`/`fallback_link` values inside the blob, inherited from the source app.
3. **Own signing keystore** (D11).
4. **Service-account JSON to LightHouse** (D8a) — without it push delivers nothing, whatever the API key says.
