# Contributing

Thank you for helping improve Never Forget.

## Good First Contributions

- Add or verify a country occasion pack with authoritative sources.
- Improve TalkBack labels, contrast, large-text behavior, or keyboard navigation.
- Add tests for calendar rules, reminder schedules, backup compatibility, or `.ics` files.
- Improve translations without changing event meaning.
- Reproduce and document device-specific reminder behavior.

## Development Setup

Requirements:

- Android Studio with Android SDK 36
- JDK 17

Run the project checks:

```bash
./gradlew test lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

## Pull Requests

1. Open an issue for large features or behavior changes.
2. Keep changes focused and preserve the offline-only privacy model.
3. Add tests for new calendar, data, reminder, import, or backup behavior.
4. Update documentation when user-visible behavior changes.
5. Run the full verification command before submitting.

Do not add analytics, advertising, remote configuration, account requirements, or an Android internet permission without prior maintainer discussion.

## Country Data

Country packs must cite authoritative sources in `docs/CALENDAR_SOURCES.md`. Include the source URL, the date verified, the applicable regions, and tests for non-fixed calendar rules.

## Privacy

Never submit real contact names, birthdays, calendar exports, backup files, signing keys, local databases, or screenshots containing personal notifications.
