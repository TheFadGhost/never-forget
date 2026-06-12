# Privacy

Never Forget 1.0.1 is a local-first Android application.

## Data storage

People, birthdays, events, reminder state, country selections, and settings are stored on the device. The app does not operate a server and does not send this information to the developer.

## Permissions

- Notifications: displays reminder notifications.
- Alarms and reminders: schedules reminders at precise times when Android allows it.
- Contacts: reads names and birthdays only when the user starts a contact import.
- Calendar: reads calendar events only when the user starts a calendar import.

Imported records become local app data. The app does not continuously monitor contacts or calendars.

## Backups

Optional backup files are encrypted with AES-256-GCM. The encryption key is derived from the user's password with PBKDF2-HMAC-SHA256. Passwords are not stored by the app and cannot be recovered.

## Network use

Version 1.0.1 has no account, analytics, advertising, telemetry, or cloud synchronization service.
