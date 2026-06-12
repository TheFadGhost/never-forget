# Never Forget Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, test, package, and privately release Never Forget 1.0.1 as a native offline-first Android calendar with persistent reminders and verified UK/Bulgaria occasions.

**Architecture:** A Kotlin/Jetpack Compose application with focused core Gradle modules and feature packages. Room is authoritative for user data, bounded generated occurrences are disposable, AlarmManager delivers precise reminders, and WorkManager/receivers repair schedules. Country packs are versioned JSON assets backed by tested recurrence rules and source provenance.

**Tech Stack:** Kotlin, Android Gradle Plugin 9.2, Gradle 9.4.1, Jetpack Compose Material 3, Room, DataStore, WorkManager, Glance, kotlinx.serialization, coroutines, JUnit, Compose UI Test, Macrobenchmark, Baseline Profiles, GitHub Actions.

---

## File Map

- `settings.gradle.kts`: plugin repositories and module graph.
- `build.gradle.kts`: root plugin declarations.
- `gradle/libs.versions.toml`: dependency catalog.
- `app/`: application, navigation, feature UI, imports, backups, widgets, and Android components.
- `core/model/`: domain entities and value types.
- `core/calendar/`: recurrence and holiday calculation.
- `core/data/`: repositories and country-pack parsing.
- `core/database/`: Room entities, DAOs, database, and migrations.
- `core/reminders/`: policies, schedule planning, alarms, notifications, and recovery.
- `core/designsystem/`: themes, typography, motion, icons, and reusable UI.
- `benchmark/`: Macrobenchmark and Baseline Profile generation.
- `.github/workflows/android.yml`: CI verification and tagged release build.
- `README.md`, `CHANGELOG.md`, `PRIVACY.md`: release documentation.

## Task 1: Project Foundation

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `.gitignore`
- Create: module build files and manifests

- [ ] Create a Gradle 9.4.1 wrapper and Android/Kotlin version catalog.
- [ ] Configure `compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`.
- [ ] Configure Java/Kotlin 21, Compose, BuildConfig, release shrinking, and test runners.
- [ ] Add modules `:app`, `:core:model`, `:core:calendar`, `:core:data`, `:core:database`, `:core:reminders`, `:core:designsystem`, and `:benchmark`.
- [ ] Run:

```powershell
.\gradlew.bat projects
```

Expected: all modules are listed and configuration succeeds.

- [ ] Commit:

```powershell
git add .
git commit -m "build: scaffold Never Forget Android project"
```

## Task 2: Domain Model And Date Rules

**Files:**
- Create: `core/model/src/main/kotlin/com/thefadghost/neverforget/model/*.kt`
- Create: `core/calendar/src/main/kotlin/com/thefadghost/neverforget/calendar/*.kt`
- Create: `core/calendar/src/test/kotlin/com/thefadghost/neverforget/calendar/*.kt`

- [ ] Write failing tests for age calculation, February 29 behavior, recurrence expansion, UK Mothering Sunday, Father's Day, Gregorian Easter, and Orthodox Easter.
- [ ] Run:

```powershell
.\gradlew.bat :core:calendar:test
```

Expected: tests fail because calculators are not implemented.

- [ ] Implement immutable types:

```kotlin
data class Person(
    val id: Long,
    val displayName: String,
    val relationship: Relationship,
    val birthday: MonthDay?,
    val birthYear: Int?
)

sealed interface RecurrenceRule {
    data object None : RecurrenceRule
    data object Yearly : RecurrenceRule
    data class YearlyNthWeekday(val month: Month, val ordinal: Int, val dayOfWeek: DayOfWeek) : RecurrenceRule
}
```

- [ ] Implement pure date calculators and bounded occurrence generation using `java.time`.
- [ ] Re-run `:core:calendar:test`; expected: all pass.
- [ ] Commit:

```powershell
git add core/model core/calendar
git commit -m "feat: add domain model and date rule engine"
```

## Task 3: Verified Country Packs

**Files:**
- Create: `core/data/src/main/assets/countries/uk.json`
- Create: `core/data/src/main/assets/countries/bg.json`
- Create: `core/data/src/main/assets/countries/bg_name_days.json`
- Create: `core/data/src/main/kotlin/com/thefadghost/neverforget/data/country/*.kt`
- Create: `core/data/src/test/kotlin/com/thefadghost/neverforget/data/country/*.kt`

- [ ] Write schema-validation and fixture tests for UK regions, Bulgarian holidays, source URLs, verification dates, aliases, and duplicate identifiers.
- [ ] Add fixed, nth-weekday, Easter-relative, Orthodox-Easter-relative, and dated-override rule formats.
- [ ] Encode the approved country events and source provenance.
- [ ] Curate a Bulgarian Name Day mapping with canonical date, Bulgarian/Latin aliases, source note, and optional multiple dates.
- [ ] Compare UK 2026/2027 fixtures to the GOV.UK feed and add fixtures to tests.
- [ ] Run:

```powershell
.\gradlew.bat :core:data:test
```

Expected: all country-pack tests pass.

- [ ] Commit:

```powershell
git add core/data
git commit -m "feat: add verified UK and Bulgaria country packs"
```

## Task 4: Room Persistence And Repositories

**Files:**
- Create: `core/database/src/main/kotlin/com/thefadghost/neverforget/database/*.kt`
- Create: `core/database/src/androidTest/kotlin/com/thefadghost/neverforget/database/*.kt`
- Create: `core/data/src/main/kotlin/com/thefadghost/neverforget/data/repository/*.kt`

- [ ] Write Room tests for people, events, checklist items, followed Name Days, occurrence state, imports, and scheduled reminders.
- [ ] Implement normalized entities and explicit foreign keys/indexes.
- [ ] Implement transactional DAOs and repository interfaces.
- [ ] Make generated occurrence caches replaceable without deleting user-authored events.
- [ ] Run:

```powershell
.\gradlew.bat :core:database:connectedDebugAndroidTest
```

Expected: DAO and transaction tests pass on the emulator.

- [ ] Commit:

```powershell
git add core/database core/data
git commit -m "feat: persist people events and reminder state"
```

## Task 5: Reminder Policy And Scheduling

**Files:**
- Create: `core/reminders/src/main/kotlin/com/thefadghost/neverforget/reminders/*.kt`
- Create: `core/reminders/src/test/kotlin/com/thefadghost/neverforget/reminders/*.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Write failing tests for 9 AM daily reminders, final-three-day 9 AM/7 PM escalation, event-day two-hour cadence, quiet hours, snooze, acknowledgment, and deterministic alarm IDs.
- [ ] Implement pure `ReminderPlanner` outputting bounded `PlannedReminder` values.
- [ ] Implement `AlarmScheduler`, exact-alarm capability checks, inexact fallback, notification channels, actions, and local diagnostics.
- [ ] Add boot, timezone, time-change, locale, package-replaced, and exact-alarm permission receivers.
- [ ] Add WorkManager audit/recovery worker.
- [ ] Run:

```powershell
.\gradlew.bat :core:reminders:test
```

Expected: all reminder-policy tests pass.

- [ ] Commit:

```powershell
git add core/reminders app/src/main/AndroidManifest.xml
git commit -m "feat: add persistent reminder scheduling"
```

## Task 6: Design System And Application Shell

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/thefadghost/neverforget/designsystem/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/NeverForgetApp.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/MainActivity.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/navigation/*.kt`

- [ ] Implement Ember, Sage, Cobalt, Monochrome, system, and OLED themes with a single accent each.
- [ ] Add Outfit font resources, tabular numeric style, custom vector icons, 48 dp controls, and reduced-motion handling.
- [ ] Implement five-tab navigation and labeled morphing quick-add speed dial.
- [ ] Add reusable empty, loading, error, permission-health, date, person, and event components.
- [ ] Add Compose tests for navigation semantics and large-font layout.
- [ ] Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

Expected: shell and accessibility tests pass.

- [ ] Commit:

```powershell
git add core/designsystem app
git commit -m "feat: add polished Compose design system and navigation"
```

## Task 7: Onboarding And Settings

**Files:**
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/onboarding/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/settings/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/settings/*.kt`

- [ ] Implement resumable onboarding backed by DataStore.
- [ ] Add country/UK-region selection, reminder defaults, theme, notification permission, exact-alarm setup, and ColorOS guidance.
- [ ] Add reminder-system health checks and direct links to relevant Android settings.
- [ ] Implement settings for quiet hours, country visibility, themes, motion, import/export, privacy, provenance, and app version.
- [ ] Test completed, denied-permission, and resumed onboarding paths.
- [ ] Commit:

```powershell
git add app
git commit -m "feat: add onboarding and reminder health settings"
```

## Task 8: Home, Calendar, People, And Occasions

**Files:**
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/home/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/calendar/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/people/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/occasions/*.kt`

- [ ] Implement Home urgency ordering, countdowns, preparation state, seven-day agenda, and health warnings.
- [ ] Implement Monday-first month grid, agenda mode, month/year navigation, search, and filters with lazy containers and stable keys.
- [ ] Implement person CRUD, relationship/custom relationship, optional birth year, leap-day preference, and linked dates.
- [ ] Implement event CRUD for birthdays, anniversaries, memorials, appointments, tasks, reminders, and custom recurrence.
- [ ] Implement country-event visibility and Bulgarian Name Day browse/follow/link flows.
- [ ] Add notes, gift ideas, budget, links, address, and checklist editing.
- [ ] Add UI tests for add/edit/search/filter/follow/complete workflows.
- [ ] Commit:

```powershell
git add app
git commit -m "feat: add calendar people occasions and home workflows"
```

## Task 9: Imports, Duplicate Merge, And Encrypted Backup

**Files:**
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/feature/importexport/*.kt`
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/backup/*.kt`
- Create: `app/src/test/kotlin/com/thefadghost/neverforget/backup/*.kt`

- [ ] Implement explicit contact birthday import with review.
- [ ] Implement one-time device-calendar import with calendar/event selection.
- [ ] Implement normalized duplicate candidates and field-by-field merge review.
- [ ] Implement versioned CSV import/export with row-level validation.
- [ ] Implement password-encrypted archive export/restore using PBKDF2-HMAC-SHA256 and AES-256-GCM with unique salts/nonces.
- [ ] Exclude secrets, alarms, and generated caches from Android backup.
- [ ] Test backup round-trip, tampering, wrong password, CSV partial errors, and duplicate candidates.
- [ ] Commit:

```powershell
git add app
git commit -m "feat: add imports merge review and encrypted backups"
```

## Task 10: Widgets And Notification Actions

**Files:**
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/widget/*.kt`
- Create: `app/src/main/res/xml/*widget*.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Implement upcoming-event and quick-add Glance widgets.
- [ ] Implement Done, Snooze, Open, Gift bought, and Plans made actions.
- [ ] Ensure annual completion only affects the current occurrence.
- [ ] Test action state transitions and widget refresh triggers.
- [ ] Commit:

```powershell
git add app
git commit -m "feat: add widgets and actionable reminders"
```

## Task 11: Performance And Hardening

**Files:**
- Create: `benchmark/src/main/kotlin/com/thefadghost/neverforget/benchmark/*.kt`
- Create: `app/src/main/baseline-prof.txt`
- Modify: Compose screens as profiling identifies jank

- [ ] Seed a benchmark-only large synthetic data set.
- [ ] Add cold-start, month fling, agenda, search, quick-add, and notification-open benchmarks.
- [ ] Generate and package a Baseline Profile.
- [ ] Verify release-like builds use R8 and resource shrinking.
- [ ] Remove main-thread date generation, unstable list keys, and unnecessary recomposition found by benchmarks.
- [ ] Run:

```powershell
.\gradlew.bat :benchmark:pixel2Api31BenchmarkAndroidTest
```

Expected: benchmark completes with frame and startup metrics; no crash or ANR.

- [ ] Commit:

```powershell
git add app benchmark
git commit -m "perf: add baseline profile and calendar benchmarks"
```

## Task 12: Documentation And CI

**Files:**
- Create: `.github/workflows/android.yml`
- Create: `README.md`
- Create: `CHANGELOG.md`
- Create: `PRIVACY.md`
- Create: `docs/COUNTRY_DATA.md`
- Create: `docs/INSTALL.md`

- [ ] Add CI jobs for unit tests, lint, debug build, instrumented emulator tests, and tagged release artifacts.
- [ ] Add signing-secret documentation without committing keys.
- [ ] Add screenshots, features, architecture, installation, privacy, roadmap, sources, changelog, and limitations.
- [ ] Ensure no open-source license file is present.
- [ ] Commit:

```powershell
git add .github README.md CHANGELOG.md PRIVACY.md docs
git commit -m "docs: add release documentation and Android CI"
```

## Task 13: Full Verification And APK

- [ ] Start API 29 and API 36 Android Studio emulators.
- [ ] Run:

```powershell
.\gradlew.bat clean test lintDebug assembleDebug connectedDebugAndroidTest
```

Expected: all tasks succeed.

- [ ] Install the debug APK and complete onboarding, add/edit/delete, import/export, notification-action, widget, rotation, process-death, timezone, and permission-denial smoke tests.
- [ ] Create an external release signing keystore and keep it outside the repository.
- [ ] Build:

```powershell
.\gradlew.bat assembleRelease
```

Expected: a signed `NeverForget-1.0.1.apk`.

- [ ] Generate SHA-256 checksum and verify the APK signature with `apksigner`.
- [ ] Commit any verification fixes.

## Task 14: Private GitHub Repository And Release

- [ ] Rename the local branch to `main`.
- [ ] Create private repository `TheFadGhost/never-forget-android`.
- [ ] Push `main`.
- [ ] Configure GitHub Actions signing secrets.
- [ ] Push tag `v1.0.1`.
- [ ] Confirm CI passes.
- [ ] Create private GitHub Release `Never Forget 1.0.1`.
- [ ] Upload signed APK and SHA-256 checksum.
- [ ] Publish release notes with Added, Changed, Fixed, and Known limitations.

## Final Acceptance

- [ ] UK regional and Bulgarian country events are visible and hideable.
- [ ] Bulgarian Name Days are browsable and selectively followable.
- [ ] Birthday age and leap-day behavior are correct.
- [ ] Reminder escalation, snooze, completion, gift, and plan actions work.
- [ ] Alarm health is visible and recovery paths run.
- [ ] Calendar and agenda remain responsive under benchmark data.
- [ ] API 29 and API 36 verification passes.
- [ ] Private GitHub repository, signed APK, checksum, and version 1.0.1 release exist.

