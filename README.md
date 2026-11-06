# Khata Book (ખાતાવહી)

Income, expense and credit (udhar) ledger for Android and the web, in Gujarati and English.

[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE) ![Languages: Gujarati + English](https://img.shields.io/badge/languages-Gujarati%20%2B%20English-blue)

**Live demo (phone and PC, no install):** https://1darshankumarsavsani-blip.github.io/Khata-Book/

## Features

- Income and expense ledger with categories and a 6-month chart
- Credit (udhar) per person, due dates, overdue marks, daily 9 AM reminder (Android)
- Statement PDF per person, shared on WhatsApp
- Monthly budgets per category with a warning at 80%
- Search and filters; Excel/CSV export
- Appearance: 8 theme colours, dark mode, 4 text sizes, bold, high contrast
- PIN lock and Google sync/backup (same data on phone and PC)

## Download

Latest release (V3.0): `Khata_Book_V3.apk` (Android, installs over older versions and keeps data) and `Khata_Book_Windows.exe` (opens the website in its own window; needs Edge or Chrome). See the [Releases page](../../releases/latest).

## Tech stack

- Android: pure Java, no third-party libraries, offline-first
- Web: single-file HTML/CSS/JavaScript
- Sync: Firebase Authentication (Google) and the Firestore REST API

## Project structure

```text
app/   Android app (Java) and unit tests
docs/  website (single index.html, GitHub Pages)
release/  release files
.github/  issue templates and release workflow
```

## Build from source

Open the `app/` project in Android Studio, or run:

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleRelease     # build the APK (sign it with your own keystore)
```

Requirements: JDK 17, Android SDK 34. The website needs no build: open `docs/index.html` in a browser.

## Privacy

Data is stored on your own device. Only if you sign in with Google does it sync to your own Google account's document in Firebase.

## Honest notes

Tested on a simulated Android 14 phone and in a headless browser. Sign-in, PDF/CSV sharing and reminders still need confirming on a real phone.

## Contributing

Bug reports and ideas are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the issue templates. Please report security problems as described in [SECURITY.md](SECURITY.md).

## License

MIT, see [LICENSE](LICENSE).

## Author

Darshankumar Savsani, DARSHAN CORPORATION, Rajkot. GitHub: [1darshankumarsavsani-blip](https://github.com/1darshankumarsavsani-blip), organization: [DARSHAN-CROP](https://github.com/DARSHAN-CROP).
