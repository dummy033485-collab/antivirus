# SafeShield — Android Antivirus & Security

An offline-first Android antivirus built with Kotlin, Jetpack Compose (Material 3), MVVM, Hilt,
Room, Coroutines/Flow, WorkManager and Retrofit.

**minSdk 26 · targetSdk 35 · compileSdk 35 · Kotlin 2.0 · AGP 8.6**

---

## 1. Why it exists

Typical free antivirus apps have four problems. SafeShield fixes each one:

| Common problem | SafeShield |
|---|---|
| Needs internet to scan | **Fully offline engine.** A signature database ships inside the APK and the heuristics are pure on-device logic. Network is 100% optional. |
| Slow, no feedback | Parallel batched coroutine scanning with **real** percentage, current item, live threat counter, measured ETA, and Pause / Resume / Cancel. |
| Intrusive ads | **Zero ads during scanning.** Exactly one small banner on the results screen, removable with a one-time IAP. |
| Weak privacy | **No personal data collected, no analytics, no accounts.** Only a SHA-256 hash leaves the device, and only if the user turns online mode on. |

---

## 2. Architecture

```
┌────────────────────────── UI (Jetpack Compose, Material 3) ──────────────────────────┐
│  MainActivity → SafeShieldApp (NavHost + bottom nav)                                  │
│  feature/home · scan · result · apps · tools · wifi · applock · junk · link · history │
│           · settings · privacy          each screen ↔ its own @HiltViewModel          │
└───────────────────────────────────────┬───────────────────────────────────────────────┘
                                        │ StateFlow / collectAsStateWithLifecycle
┌───────────────────────────────────────▼───────────────────────────────────────────────┐
│ DOMAIN (pure Kotlin, unit-tested, no Android framework in the scoring classes)        │
│   ScanEngine ── orchestrates signature + heuristic + optional online checks           │
│   HeuristicAnalyzer · RiskScorer · SecurityScoreCalculator · models                   │
└───────────────────────────────────────┬───────────────────────────────────────────────┘
┌───────────────────────────────────────▼───────────────────────────────────────────────┐
│ DATA                                                                                   │
│   local/   Room: signatures, scan_records, threat_records, whitelist, locked_apps      │
│   prefs/   DataStore: theme, language, online mode, schedule, salted PIN hash          │
│   remote/  Retrofit: VirusTotal v3 (hash lookup ONLY), Safe Browsing v4, signature feed│
│   repository/ AppInventory · Scan · Signature · VirusTotal · Wifi · Junk · Link ·      │
│               AppLock · Billing                                                        │
└───────────────────────────────────────┬───────────────────────────────────────────────┘
┌───────────────────────────────────────▼───────────────────────────────────────────────┐
│ PLATFORM                                                                               │
│   service/  ScanForegroundService (dataSync) + ScanController (process-wide state)     │
│             NotificationHelper (two channels: progress / threats)                      │
│   receiver/ PackageEventReceiver (real-time), BootCompletedReceiver (re-arm work)      │
│   worker/   ScheduledScanWorker · PackageScanWorker · ApkWatchWorker ·                 │
│             SignatureUpdateWorker · WorkScheduler                                      │
│   di/       AppModule (Room, Retrofit, Moshi, OkHttp, dispatchers)                     │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

**Key design decisions**

* `ScanController` is a `@Singleton` holding the running scan, so rotation or navigating away
  never restarts or loses a scan. The foreground service only keeps the process alive.
* Scoring classes take plain data classes as input, so they are tested with JUnit alone — no
  Robolectric, no emulator.
* The Retrofit interface for VirusTotal declares **only** `GET files/{hash}`. There is no upload
  endpoint in the codebase, so a file cannot leak even by accident.
* Custom Scan uses the Storage Access Framework (`OpenDocumentTree`) instead of
  `MANAGE_EXTERNAL_STORAGE`, which Google Play rejects for antivirus apps that are not
  file managers.

---

## 3. Folder structure

```
SafeShield/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml              # version catalog
├── local.properties.example               # copy → local.properties, add API keys
├── gradlew · gradlew.bat · gradle/wrapper/
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/malware_signatures.json       # bundled offline hash DB
        │   ├── res/
        │   │   ├── values/{strings,colors,themes}.xml
        │   │   ├── values-ur/strings.xml            # full Urdu translation
        │   │   ├── xml/{file_paths,data_extraction_rules,
        │   │   │        accessibility_service_config,locales_config}.xml
        │   │   ├── drawable/ · mipmap-anydpi-v26/
        │   └── java/com/safeshield/app/
        │       ├── SafeShieldApp.kt
        │       ├── core/            Hashing · Formatters · Constants · DispatcherProvider
        │       ├── di/              AppModule
        │       ├── domain/
        │       │   ├── model/       Models.kt
        │       │   └── engine/      ScanEngine · HeuristicAnalyzer · RiskScorer ·
        │       │                    SecurityScoreCalculator
        │       ├── data/
        │       │   ├── local/       SafeShieldDatabase · Converters · entity/ · dao/ · json/
        │       │   ├── prefs/       SettingsRepository
        │       │   ├── remote/      api/ · dto/
        │       │   └── repository/  AppInventory · Scan · Signature · VirusTotal · Wifi ·
        │       │                    Junk · LinkCheck · AppLock · Billing
        │       ├── service/         ScanController · ScanForegroundService · NotificationHelper
        │       ├── receiver/        PackageEventReceiver · BootCompletedReceiver
        │       ├── worker/          ScheduledScan · PackageScan · ApkWatch · SignatureUpdate ·
        │       │                    WorkScheduler
        │       ├── ui/              MainActivity · MainViewModel · navigation/ · theme/ ·
        │       │                    components/
        │       └── feature/         home · scan · result · apps · tools · wifi · applock ·
        │                            junk · link · history · settings · privacy
        └── test/java/com/safeshield/app/
            ├── ScanEngineTest.kt
            ├── RiskScorerTest.kt
            ├── HeuristicAnalyzerTest.kt
            ├── SecurityScoreCalculatorTest.kt
            ├── HashingTest.kt
            └── FormattersTest.kt
```

---

## 4. Features

**Scan engine** — Quick (installed apps), Full (apps + APKs in storage), Custom (a SAF folder),
Realtime (one package). Detection layers, in order:
1. streaming SHA-256 of each APK compared against the Room signature DB,
2. heuristics — SMS + overlay combo, sideloaded dropper, hidden launcher icon, device-admin
   abuse, accessibility abuse, debuggable release, very old `targetSdk`,
3. optional VirusTotal hash lookup (≥3 detections = HIGH, ≥10 = CRITICAL).

**Live scan UI** — percentage, current file/app, scanned counter, threat counter, ETA from
measured throughput, Pause/Resume/Cancel, runs under a foreground notification.

**Permission Risk Analyzer** — every app scored 0–100 with Low/Medium/High and plain-language
explanations ("Can read your SMS codes and draw a fake screen over other apps"). Actions:
App info, Uninstall, Trust (whitelist).

**Real-time protection** — `PACKAGE_ADDED`/`PACKAGE_REPLACED` → `PackageScanWorker` → notification.
`ApkWatchWorker` sweeps Downloads for new APKs every 6 h.

**WiFi security** — rates the current network (OPEN / WEP / WPA / WPA2 / WPA3) 0–100.

**App Lock** — BiometricPrompt + salted-hash PIN, driven by Usage Access **or** Accessibility
(user's choice, both explained in plain language).

**Junk cleaner** — honest: deletes only SafeShield's own cache, and *reports* other apps' cache
sizes (via `StorageStatsManager`) while opening the system screen for the user to clear them.
No fake "1.2 GB cleaned".

**Link checker** — Google Safe Browsing v4; also registered for `ACTION_SEND` so a link can be
shared into the app.

**History & scheduling** — Room history, text report export to app-private storage, daily/weekly
`PeriodicWorkRequest`.

**Settings** — theme, English/Urdu, online mode, real-time toggle, schedule, signature update,
Remove ads IAP, **Delete all my data**.

---

## 5. Setup

### Prerequisites
* Android Studio Ladybug (2024.2.1) or newer
* JDK 17
* Android SDK Platform 35, Build-Tools 35.x

### Steps

```bash
git clone <this-repo>
cd SafeShield
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/Users/you/Library/Android/sdk

# Optional — the app is fully functional offline without these.
VIRUSTOTAL_API_KEY=<your VirusTotal v3 key>
SAFE_BROWSING_API_KEY=<your Google Safe Browsing key>

# AdMob. The defaults below are Google's official TEST ids — replace before release.
ADMOB_APP_ID=ca-app-pub-3940256099942544~3347511713
ADMOB_BANNER_ID=ca-app-pub-3940256099942544/6300978111

# Release signing (optional, only needed for a signed build)
RELEASE_STORE_FILE=/abs/path/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

`local.properties` is git-ignored; keys are injected into `BuildConfig` at compile time and never
committed. CI can supply the same names as environment variables instead.

Getting keys:
* **VirusTotal** — free account at virustotal.com → *API key*. Public keys allow 4 requests/min;
  `VirusTotalRepository` already caps concurrency at 4 and caches results.
* **Safe Browsing** — Google Cloud Console → enable *Safe Browsing API* → create an API key.

> If the gradle wrapper jar is absent, run `gradle wrapper --gradle-version 8.9` once, or just
> open the project in Android Studio, which regenerates it.

---

## 6. Build & run

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:installDebug           # install on a connected device
./gradlew test                        # unit tests (engine + scoring)
./gradlew :app:lint                   # Android lint
./gradlew :app:assembleRelease        # minified, shrunk, signed (if keystore configured)
./gradlew :app:bundleRelease          # AAB for Play Console
```

Outputs:
* `app/build/outputs/apk/debug/app-debug.apk`
* `app/build/outputs/apk/release/app-release.apk`
* `app/build/outputs/bundle/release/app-release.aab`

### Verifying detection works
`assets/malware_signatures.json` contains a harmless self-test signature named
`SafeShield.Test.File`. Create a file whose content is exactly
`SAFESHIELD-TEST-FILE-DO-NOT-PANIC`, rename it to `test.apk`, put it in Downloads and run a
Full Scan — it must be reported as a LOW-severity signature hit.

### Updating the signature database
Edit `app/src/main/assets/malware_signatures.json` (bump `version`) or host the same JSON shape at
`Constants.SIGNATURE_FEED_URL`; `SignatureUpdateWorker` pulls it daily on unmetered networks and
only applies feeds with a higher `version`.

---

## 7. Permissions — what and why

| Permission | Why | User-facing handling |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Core antivirus function: enumerate installed packages to hash their APKs | Declared in Play Console as *Antivirus / security* core functionality |
| `POST_NOTIFICATIONS` | Threat alerts and scan progress (Android 13+) | Requested on first Home-screen view |
| `FOREGROUND_SERVICE` + `_DATA_SYNC` | Keep a scan alive in the background | Visible ongoing notification |
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | Legacy Full Scan on API 26–32 | API 33+ uses SAF instead |
| `ACCESS_WIFI_STATE` + `ACCESS_COARSE_LOCATION` | OS requires location to read WiFi capabilities | Explained on the WiFi screen; never stored or sent |
| `PACKAGE_USAGE_STATS` | App Lock foreground detection + cache sizes | Special access; user grants in Settings |
| `BIND_ACCESSIBILITY_SERVICE` | Optional alternative App Lock trigger | Service only reads the foreground package name; disclosed in-app and in the service description |
| `SYSTEM_ALERT_WINDOW` | Show the lock screen over a locked app | Optional |
| `RECEIVE_BOOT_COMPLETED` | Re-arm scheduled scans after reboot | — |

**Deliberately NOT requested:** `MANAGE_EXTERNAL_STORAGE`, contacts, SMS, call log, camera,
microphone, precise location.

---

## 8. Privacy & Play Data Safety

SafeShield collects **no** personal or sensitive user data, has no analytics SDK, no crash
reporter and no account system. Everything is processed on-device.

What to declare in the **Play Console → Data Safety** form:

* **Data collected:** *None.*
* **Data shared:** *None.* (A SHA-256 hash is not personal data and is not linked to a user or
  device; still, disclose the VirusTotal/Safe Browsing integrations in your privacy policy.)
* **Data encrypted in transit:** Yes — all network calls are HTTPS.
* **Users can request data deletion:** Yes — *Settings → Delete all my data* wipes Room, DataStore
  and the PIN immediately, with no server-side copy to delete.
* **Security practices:** app is not backed up to the cloud
  (`allowBackup=false`, explicit `data_extraction_rules.xml`).

Other Play policy notes:
* **Accessibility API** — used only to read the foreground package name for App Lock. The purpose
  is disclosed in `accessibility_service_config.xml`, on the App Lock screen, and must also be
  disclosed in the store listing and in an in-app prominent disclosure.
* **QUERY_ALL_PACKAGES** — permitted for "device security (anti-virus, anti-malware)"; select
  that declaration in the console.
* **Ads** — one banner, results screen only, never during a scan; declare the app as containing ads.
* **Malware naming** — never claim to remove a threat the app cannot remove; SafeShield always
  routes removal through the system uninstall dialog.

---

## 9. Testing

```bash
./gradlew test
```

Covers: hash correctness (known vectors, missing file), risk scoring (boundaries, clamping,
system-app damping), heuristics (clean app, banking-trojan combo, dropper, system exemption),
security score (fresh/stale/never scanned, clamping) and the scan engine (signature hit,
signature-wins-over-online, online opt-in gating, unreadable file).

## 10. License

Released for the project owner's use. Third-party components keep their own licenses
(Apache 2.0 for AndroidX, Hilt, Retrofit, OkHttp, Moshi).
