# Never Forget Android Design Specification

**Status:** Approved for implementation planning

**Version:** 1.0.1

**Package:** `com.thefadghost.neverforget`

**Repository:** Private GitHub repository named `never-forget-android`

**Primary device:** OPPO Find X9 Pro running ColorOS 16.0.7

**Platform:** Android 10 (API 29) and newer, targeting Android 16

## Product Summary

Never Forget is an offline-first Android calendar and reminder application for
important personal dates. Its defining behavior is an intentionally persistent
reminder system for birthdays, family occasions, major observances, public
holidays, and selected Bulgarian Name Days.

Version 1.0.1 focuses on the United Kingdom and Bulgaria. UK coverage is split
into England and Wales, Scotland, and Northern Ireland. The country-data
architecture must allow additional country packs in future releases without
changing the personal-event model.

The app stores personal data locally, requires no account, contains no
advertising or analytics, and does not depend on Google Calendar after an
optional one-time import.

## Goals

- Make important dates difficult to overlook without becoming impossible to
  control.
- Provide a fast calendar, agenda, people directory, and quick-add workflow.
- Bundle auditable UK and Bulgarian holiday and observance rules.
- Display the full Bulgarian Name Day calendar while notifying only for names
  the user explicitly follows.
- Work offline after installation.
- Remain smooth during calendar and agenda scrolling on high-refresh devices.
- Survive app restarts, process death, reboot, timezone changes, and app updates.
- Produce a signed APK and professional private GitHub release for version
  1.0.1.

## Non-Goals

- iOS support.
- Cloud accounts or cross-device synchronization.
- Continuous Google Calendar synchronization.
- Email or social messaging.
- Do Not Disturb bypass.
- A guarantee that ColorOS will always run the display at 120 Hz.
- A guarantee that reminders work when the user denies Android notification or
  exact-alarm access.
- Complete curated observance data for countries other than the UK and Bulgaria.
- Google Play publication in version 1.0.1.

## Product Architecture

The application is a native Kotlin project using Jetpack Compose and Material 3.
The app follows a single-activity architecture with feature-oriented packages
and unidirectional data flow.

Core technologies:

- Kotlin and coroutines.
- Jetpack Compose and Navigation Compose.
- Room for structured application data.
- DataStore for preferences and onboarding state.
- AlarmManager for user-visible reminder times.
- WorkManager for schedule audits and recovery work.
- App Widgets using Glance.
- Android Contacts and Calendar providers for explicit one-time imports.
- Android Keystore-backed cryptography for password-encrypted backups.
- JUnit, coroutine test utilities, Room tests, Compose UI tests, Macrobenchmark,
  and Baseline Profiles.

The source tree will be split into focused modules:

- `app`: Android entry point, dependency wiring, top-level navigation, and
  manifest components.
- `core:model`: shared domain types.
- `core:database`: Room schema, DAOs, and migrations.
- `core:designsystem`: themes, typography, icons, motion, and reusable controls.
- `core:calendar`: recurrence, age, holiday, and occurrence generation.
- `core:reminders`: reminder policy, alarm scheduling, receivers, and
  notification actions.
- `core:data`: repositories and bundled country-pack access.
- `feature:onboarding`
- `feature:home`
- `feature:calendar`
- `feature:people`
- `feature:occasions`
- `feature:settings`
- `feature:importexport`
- `widget`
- `benchmark`

Each feature owns its screens and presentation logic. Domain rules remain in
core modules so they can be tested without Android UI dependencies.

## Navigation And Information Architecture

The bottom navigation contains:

1. Home
2. Calendar
3. People
4. Occasions
5. Settings

A prominent floating add button is available from primary screens. It expands
into Birthday, Event, Reminder, Person, and Task actions. The speed dial uses
clear labels in addition to icons.

### Home

Home prioritizes:

- Today's unresolved events.
- Urgent countdowns.
- Preparation items such as gifts and plans.
- The next seven days.
- Permission or scheduling health warnings.

### Calendar

Calendar provides:

- Monday-first month view.
- Agenda view.
- Smooth navigation by month and year.
- Search.
- Filters by event type, country, region, person, relationship, and completion.
- Hidden-country-event controls.

### People

People provides:

- Manual creation.
- Optional contact import.
- Relationships: partner, mother, father, sibling, child, grandparent, friend,
  colleague, relative, and custom.
- Birthdays with an optional birth year.
- Optional Name Day links.
- Duplicate review and merge.

### Occasions

Occasions provides:

- Enabled country packs and regions.
- Public holidays and cultural observances.
- Bulgarian Name Day browsing and selection.
- Custom anniversaries, memorial dates, appointments, tasks, and recurring
  events.
- Per-event reminder and visibility settings.

### Settings

Settings provides:

- Reminder defaults and quiet hours.
- Notification and exact-alarm health.
- ColorOS setup guidance.
- Theme and motion preferences.
- Country and region selection.
- Import, export, backup, restore, and data deletion.
- Privacy, source provenance, app version, and open-source acknowledgements.

## Onboarding

Onboarding is resumable and contains:

1. Product explanation.
2. Primary country selection.
3. Additional country and UK-region selection.
4. Reminder behavior and default times.
5. Notification permission explanation and request.
6. Exact-alarm capability explanation and setup.
7. ColorOS notification, auto-launch, background activity, and battery guidance.
8. Optional contacts import.
9. Optional device-calendar import.
10. Theme selection.
11. Completion summary with a reminder-system health check.

Contacts and calendar permissions are requested only after the user selects the
corresponding import action. Denied permissions never block manual use.

## Visual Design

The visual direction is clean Material 3 with restrained asymmetry and moderate
motion. It avoids excessive containers, decorative gradients, glowing effects,
and generic card grids. Elevation is used only when it communicates functional
hierarchy.

Typography uses Outfit for the interface, with a platform-safe sans-serif
fallback. Numeric countdowns use tabular figures.

The default theme is Ember:

- Warm neutral background and surfaces.
- Charcoal text rather than pure black.
- One muted coral accent.
- Subtle tinted elevation.

Additional themes:

- Sage.
- Cobalt.
- Monochrome.
- System light/dark.
- OLED dark.

Each active theme has one accent color. The app supports dynamic system bars,
large text, screen readers, sufficient contrast, touch targets of at least 48
dp, and Android's reduced-motion preference.

Motion uses spring-like shared transitions, staggered entry for short lists,
morphing add dialogs, and tactile press feedback. Continuous decorative
animation is avoided. Animations change transform or opacity rather than layout
dimensions, and no animation is allowed to interfere with scrolling.

## Personal Event Model

The application supports:

- Birthdays.
- Anniversaries.
- Memorial dates.
- Appointments.
- Tasks.
- General reminders.
- Fully custom events.
- Country observances.
- Name Days.

An event can include:

- Title and optional linked person.
- Date and optional time.
- Optional birth year or start year.
- Timezone behavior.
- Recurrence rule.
- Relationship.
- Notes.
- Gift ideas.
- Optional gift budget.
- Links.
- Address.
- Checklist.
- Country and region classification.
- Visibility.
- Reminder policy.
- Preparation state.

Birthdays require month and day. Birth year is optional. When no birth year is
stored, the app omits age rather than estimating it. February 29 birthdays are
observed on February 28 in non-leap years by default, with a per-event override
available for March 1.

Completing an annual event resolves only that year's occurrence. It does not
disable future years.

## Reminder Policy

The default reminder policy is:

- Birthdays: start 14 days before.
- Valentine's Day and Christmas: start 21 days before.
- Mother's Day and Father's Day: start 14 days before.
- Easter and selected Name Days: start 7 days before.
- Other events: start 7 days before unless the user changes the default.

During the lead period, a reminder fires daily at 9:00 AM.

During the final three days, reminders fire at 9:00 AM and 7:00 PM.

On the event day, an unresolved reminder fires every two hours from 9:00 AM
through 9:00 PM.

Acknowledgment actions are:

- Done.
- Snooze.
- Open event.

Snooze options are:

- 15 minutes.
- 1 hour.
- 3 hours.
- Tomorrow morning.

Preparation actions are:

- Gift bought.
- Plans made.

Preparation actions update the event state but do not acknowledge or silence the
event-day reminder.

Quiet hours are 9:00 PM through 9:00 AM. A user can explicitly override quiet
hours for an individual event. Aggressive reminders can be disabled or
customized per event.

The notification design uses standard high-importance notification channels and
action buttons. It does not request Do Not Disturb access or abuse full-screen
intents.

## Reminder Scheduling And Recovery

The reminder engine generates occurrences and schedules a bounded rolling
window. It must not schedule every future reminder for all years at once.

AlarmManager is used for precise user-visible alerts. The implementation checks
exact-alarm capability before scheduling and presents a visible degraded state
when exact alarms are unavailable. Inexact fallback alarms retain best-effort
functionality.

WorkManager performs periodic schedule audits and repairs missing future alarms.

Receivers reschedule relevant reminders after:

- Device boot.
- Timezone changes.
- Time or date changes.
- Locale changes where display text is affected.
- App replacement or update.
- Exact-alarm permission changes.

Every scheduled alarm has a deterministic identity so updates and cancellation
cannot create duplicate notifications.

Settings and Home display reminder-system health. Silent scheduling failure is
not acceptable.

## Country Packs

Country packs are versioned, offline assets. A pack contains:

- Pack identifier and semantic data version.
- Country and optional region.
- Display names.
- Event classification.
- Fixed-date or calculated recurrence rule.
- Substitute-day behavior.
- Default reminder category.
- Source authority.
- Source URL.
- Source verification date.
- Notes about exceptional or one-off dates.

Country packs generate events for a bounded year range and can be updated by app
releases. One-off government holidays are stored as dated overrides. They are
never inferred as permanent recurrence rules.

### United Kingdom

Users may independently enable:

- England and Wales.
- Scotland.
- Northern Ireland.

The official GOV.UK Bank Holidays API is the primary source for confirmed bank
holiday dates and regional differences. The app bundles verified offline data
and rules so it remains functional without a network connection. Release
preparation compares bundled data against the official feed.

The UK pack includes:

- New Year's Day.
- 2 January in Scotland.
- St Patrick's Day in Northern Ireland.
- Good Friday.
- Easter Monday where applicable.
- Early May bank holiday.
- Spring bank holiday.
- Battle of the Boyne in Northern Ireland.
- Summer bank holiday with regional rules.
- St Andrew's Day in Scotland.
- Christmas Day.
- Boxing Day.
- Substitute bank holidays.
- Valentine's Day.
- Mothering Sunday, the fourth Sunday of Lent.
- Father's Day, the third Sunday of June.
- Halloween.
- St George's Day as a cultural observance rather than a UK-wide public holiday.

### Bulgaria

The official-holiday source of truth is Article 154 of the Bulgarian Labour Code
and the Bulgarian Council of Ministers' public-holiday guidance.

The Bulgaria pack includes:

- New Year's Day.
- Liberation Day on March 3.
- Labour Day on May 1.
- St George's Day and Bulgarian Armed Forces Day on May 6.
- Day of the Holy Brothers Cyril and Methodius, the Bulgarian Alphabet,
  Education and Culture, and Slavic Literature on May 24.
- Unification Day on September 6.
- Independence Day on September 22.
- National Awakeners Day on November 1, marked as an educational holiday.
- Christmas Eve.
- Christmas Day and the second day of Christmas.
- Orthodox Good Friday, Holy Saturday, Easter Sunday, and Easter Monday.
- Statutory substitute non-working days.
- Valentine's Day as a cultural/commercial observance.
- International Women's Day and commonly observed Mother's Day on March 8.
- Father's Day on December 26 as an observance.
- Halloween as an optional cultural observance.

Orthodox Easter dates are generated with a tested Orthodox computus and checked
against the Bulgarian Orthodox Church calendar for the release year range.

### Bulgarian Name Days

The app displays the full Bulgarian Orthodox calendar and its curated name
associations. The canonical calendar source is the Bulgarian Orthodox Church.
Because the church calendar lists commemorations rather than a complete
consumer-oriented name-to-date mapping, the app maintains a reviewed mapping
layer with provenance and aliases.

Name Day notifications are disabled by default. A user may:

- Follow a name without linking a person.
- Link a followed name to an existing person.
- Create a person while selecting a name.
- Edit aliases or choose among multiple valid dates.
- Hide individual calendar entries.

The interface recommends linking a contact but does not require it.

## Data Storage

Room entities cover:

- People.
- Events.
- Recurrence rules.
- Generated occurrences.
- Reminder policies.
- Scheduled reminders.
- Occurrence completion.
- Preparation actions.
- Checklists.
- Country-pack selections.
- Followed Name Days.
- Import batches.
- Source provenance.

Generated occurrences are cached only for a bounded window and can be rebuilt
from source events and country rules. User-authored data is authoritative;
generated cache data is disposable.

DataStore contains:

- Onboarding state.
- Theme and motion preferences.
- Default reminder policy.
- Quiet hours.
- Enabled countries and regions.
- Permission-health dismissal state.
- Import preferences.

All schema changes require explicit Room migrations and migration tests.

## Import, Export, And Backup

### Contacts

Contact import reads selected contact names and birthdays. The user reviews all
proposed records before saving. The app does not continuously monitor contacts.

### Device Calendar

Calendar import is a one-time copy. The user chooses calendars and events,
reviews them, and imports them into Never Forget. Imported events are then
independent of the source calendar.

### Duplicate Detection

Potential duplicates are detected using normalized name and date, with
relationship and source identifiers as secondary evidence. A merge screen shows
field-by-field differences. No destructive automatic merge occurs.

### CSV

CSV import and export use a documented, versioned schema. Import validation
reports row-level errors without discarding valid rows.

### Encrypted Backup

The app exports a versioned backup archive encrypted with a user-provided
password. Encryption uses a modern password-based key derivation function,
authenticated encryption, a unique salt, and a unique nonce. The password is
never stored.

Restore validates archive version and integrity before modifying the database.
Restore supports replacing all local data or merging through duplicate review.

Android automatic device backup is enabled only for non-secret preferences and
approved application data. Export passwords, cryptographic keys, transient
alarms, and restorable generated caches are excluded.

## Performance

The app targets smooth rendering on high-refresh displays but does not claim
control over OEM refresh-rate policy.

Performance requirements:

- Use lazy lists and stable item keys.
- Keep expensive date generation and filtering off the main thread.
- Use immutable UI models and stable state.
- Avoid reading fast-changing state high in the Compose tree.
- Precompute month cells and occurrence summaries.
- Cache only bounded date ranges.
- Avoid continuous decorative animation.
- Ship an application-specific Baseline Profile.
- Enable R8 optimization and resource shrinking for release builds.
- Set a high-refresh preference where the platform API and device permit it.

Benchmark journeys:

- Cold start to interactive Home.
- Open month calendar.
- Fling through twelve months.
- Open agenda with a large data set.
- Search people and events.
- Expand quick-add and save a birthday.
- Open an event from a notification.

Frame timing, startup time, allocations, and jank are measured in release-like
builds. Debug rendering performance is not treated as release performance.

## Accessibility

- Content descriptions for meaningful icons.
- Text labels for navigation and speed-dial actions.
- Logical TalkBack traversal.
- Dynamic type without clipping at large font scales.
- Minimum 48 dp interaction targets.
- Contrast validated in every theme.
- Color is never the sole status indicator.
- Reduced motion disables nonessential transitions.
- Date and time controls expose localized spoken values.

The UI language is English in version 1.0.1. Bulgarian event names may appear as
secondary labels where useful.

## Error Handling

The app uses actionable inline errors and persistent health states:

- Permission denial explains the missing capability and links to system
  settings.
- Exact-alarm denial activates and labels best-effort mode.
- Import failures identify the affected row or source event.
- Restore failure leaves the current database unchanged.
- Country data errors omit only invalid entries and report the affected pack in
  diagnostics.
- Scheduling errors are recorded locally and surfaced in reminder health.
- Empty states explain how to add or import data.

The app never claims that a reminder is protected when required system access is
missing.

## Testing Strategy

### Unit Tests

- Gregorian and Orthodox Easter calculations.
- UK regional bank-holiday rules and confirmed fixtures.
- Bulgarian statutory holidays and substitute days.
- Mothering Sunday and Father's Day recurrence.
- Leap-year and February 29 birthday behavior.
- Age calculations with and without a birth year.
- Recurrence expansion and timezone behavior.
- Reminder lead periods, quiet hours, escalation, snooze, and acknowledgment.
- Deterministic alarm identifiers.
- Country-pack parsing and provenance validation.
- Duplicate detection.
- CSV validation.
- Encrypted backup round trips and wrong-password failure.

### Database Tests

- DAO behavior.
- Transactions.
- Merge behavior.
- Every Room migration.
- Cache regeneration after migration.

### UI And Accessibility Tests

- Complete onboarding.
- Permission denial and recovery.
- Add and edit a birthday.
- Add a custom recurring event.
- Follow and unfollow a Name Day.
- Search and filter.
- Import review and duplicate merge.
- Complete, snooze, open, gift-bought, and plans-made actions.
- Theme switching.
- Large fonts and TalkBack semantics.

### System And Reliability Tests

- Process death.
- Device reboot.
- Timezone and manual clock changes.
- Locale changes.
- App update.
- Notification permission revocation.
- Exact-alarm permission revocation and grant.
- Battery-saver mode.
- Alarm duplication prevention.

### Performance Tests

- Macrobenchmark critical journeys.
- Baseline Profile generation and validation.
- Startup timing.
- Calendar and agenda jank.
- Memory and allocation checks with large synthetic data.

The configured Android Studio emulators are used for API 29 and API 36 testing.
Emulators cannot reproduce all OPPO ColorOS background-management behavior or
prove physical 120 Hz rendering. Those limitations must remain in release notes
until a physical OPPO test is performed.

## Build, Security, And Release

The project uses the Android Studio bundled JDK 21 and a Gradle wrapper committed
to the repository.

GitHub Actions performs:

- Static analysis.
- Unit tests.
- Android lint.
- Debug APK build.
- Instrumented tests where emulator runners are available.
- Release build on a version tag.
- Checksum generation.

The signing keystore and passwords are never committed. Local release builds use
an external keystore. GitHub Actions uses encrypted repository secrets.

The private GitHub repository contains:

- Professional README.
- Installation instructions for sideloading.
- Screenshots.
- Feature list.
- Privacy statement.
- Architecture summary.
- Country-data source policy.
- Roadmap.
- Changelog.
- Known limitations.
- Contribution guidance for future private collaborators.

No open-source license is added.

The version 1.0.1 GitHub Release contains:

- Signed universal APK.
- SHA-256 checksum.
- Release notes grouped into Added, Changed, Fixed, and Known limitations.

## Verified Source Policy

Initial authoritative sources:

- GOV.UK regional bank holidays:
  <https://www.gov.uk/bank-holidays>
- GOV.UK Bank Holidays API:
  <https://www.gov.uk/bank-holidays.json>
- Bulgarian Council of Ministers public holidays:
  <https://government.bg/en/About-Bulgaria/BULGARIAN-PUBLIC-HOLIDAYS>
- Bulgarian Labour Code, Article 154:
  <https://www.lex.bg/bg/laws/ldoc/1594373121>
- Bulgarian National Assembly holiday summary:
  <https://www.parliament.bg/en/24>
- Bulgarian Orthodox Church calendar:
  <https://bg-patriarshia.bg/calendar>
- Church of England Mothering Sunday rule:
  <https://www.churchofengland.org/media/stories-and-features/mothering-sunday-what-are-its-origins-church>
- Android exact-alarm behavior:
  <https://developer.android.com/about/versions/14/changes/schedule-exact-alarms>
- Android notification permission:
  <https://developer.android.com/develop/ui/views/notifications/notification-permission>
- Android frame-rate guidance:
  <https://developer.android.com/media/optimize/performance/frame-rate>
- Jetpack Compose performance:
  <https://developer.android.com/develop/ui/compose/performance>

Church text and site content are not copied into the app. The project stores
facts, calculated dates, short event labels, and source citations. Any curated
name alias mapping must be original project data derived through review rather
than a copied third-party proprietary list.

Before each release, maintainers must:

1. Compare UK bundled dates with the official GOV.UK feed.
2. Check Bulgarian government announcements for one-off holidays.
3. Verify Orthodox Easter and the release-year church calendar.
4. Record the verification date and any overrides in the country-pack metadata.

## Acceptance Criteria

Version 1.0.1 is complete when:

- A new user can finish onboarding and understand reminder-system health.
- UK regions and Bulgaria can be enabled together.
- Verified country events appear every supported year.
- Bulgarian Name Days are browsable and selectively followable.
- Birthdays can be entered with or without a birth year.
- The default reminder escalation and all notification actions work.
- Upcoming reminders are restored after reboot, timezone change, and app update.
- Contacts and calendars can be imported through review screens.
- Encrypted backup, restore, CSV import, and CSV export pass tests.
- Month and agenda scrolling meet benchmark targets without material jank on the
  configured API 36 emulator.
- API 29 and API 36 test suites pass.
- The private GitHub repository is documented professionally.
- GitHub Actions passes.
- A signed version 1.0.1 APK, checksum, and release notes are published in a
  private GitHub Release.

