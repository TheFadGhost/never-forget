# Security Policy

## Supported Version

Security fixes are applied to the latest release on `main`.

## Reporting

Use GitHub private vulnerability reporting when it is available for this repository. Otherwise, contact the maintainer privately through the repository owner's GitHub profile.

Do not open a public issue containing:

- Backup files or passwords
- Real contacts, birthdays, or calendar data
- Signing keys or credentials
- A working exploit that exposes local user data

## Security Model

Never Forget is local-first and has no backend, account system, analytics, advertising, or internet permission. Sensitive areas include encrypted backup handling, Android file imports, Room migrations, reminder intents, and contact/calendar permission boundaries.

Encrypted backups use AES-256-GCM with a key derived from the user password through PBKDF2-HMAC-SHA256. Passwords are not stored.
