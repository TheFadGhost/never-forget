# Never Forget Open-Source Release Design

## Goal

Publish Never Forget as a privacy-safe, actively maintained Android open-source project with a fresh Git history and useful calendar interoperability.

## Public Repository

- Repository name: `never-forget`
- License: MIT
- Source history: one fresh public initial commit
- Original private repository: unchanged
- Runtime model: offline-first, no account, analytics, advertising, or backend

## Calendar Interoperability

Add RFC 5545-compatible `.ics` import and export for user-managed events.

The codec will:

- Export visible non-country events as `VEVENT` records.
- Preserve title, notes, date, optional time, yearly recurrence, event type, and reminder lead days.
- Escape commas, semicolons, backslashes, and newlines.
- Import all-day and local date-time events.
- Unfold continued lines and ignore unsupported properties.
- Report malformed events without discarding valid events in the same file.
- Avoid duplicate imports using normalized title, date, and time.

Country-pack observances will not be exported because they can be regenerated from the selected country packs.

## Open-Source Maintenance

The repository will include:

- MIT license
- Contribution guide
- Code of conduct
- Security policy
- Issue and pull-request templates
- Android CI for tests, lint, and debug APK assembly
- A roadmap focused on additional country packs, accessibility, translations, and reminder reliability

## Privacy Controls

The public copy will exclude:

- Private Git history
- Signing files and credentials
- Local Android SDK configuration
- Build outputs and generated reports
- Real contacts, calendars, backups, databases, or notification data
- Machine-specific paths and private deployment configuration

## Verification

Required checks:

1. Calendar codec unit tests pass.
2. Full Gradle unit tests pass.
3. Android lint passes.
4. Debug APK assembles.
5. Secret and personal-path scans return no actionable findings.
6. GitHub repository is public and contains only the fresh public history.
