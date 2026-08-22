# Changelog

All notable changes to ArvanShare are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.0.0-beta.2] — 2026-08-22

### Changed
- **Android UI readability pass** (all screens, both themes)
  - Brighter secondary text colour in dark mode, darker in light mode — timestamps,
    likes lists, hints and errors are clearly legible now.
  - Explicit `outline` colours so text-field borders stand out.
  - Base small-text size raised from 12sp to 13sp.

### Fixed
- Avatar initials were nearly invisible on bright palette entries (e.g. white on
  orange); the palette now uses darker shades that keep white text readable.
- "Comments" heading no longer styled dimmer than body text.
- Post editor and comment box use persistent floating labels instead of
  disappearing placeholders.
- Setup description and connection-result chip enlarged to 14sp.

### Repository
- Repaired a corrupted (UTF-16) `gradle.properties`.
- Ignore `base64_keystore.txt` / `job_log.txt` so signing material can never be committed.

## [1.0.0-beta.1] — 2026-08-08

### Added
- **Android app** (Kotlin + Jetpack Compose)
  - Full feed, compose, post-detail, and first-run setup screens.
  - Custom indigo/teal Material3 theme with dark & light mode.
  - Author avatar circles (color-coded by name hash) on all screens.
  - Spring-animated like button with colour transition.
  - Per-card slide-in entrance animation on the feed.
  - Adaptive launcher icon (`AS` monogram on indigo gradient).
  - Room database for offline-first feed cache.
  - DataStore settings (name, endpoint, bucket, keys) — stored only on device.
  - Signed release APK via Gradle env-var signing config.
  - ProGuard + resource shrinking enabled for release builds.

- **Windows desktop app** (Python + Tkinter)
  - Dark-mode UI (`#12131A` base) with configurable `THEME` token dict.
  - Avatar canvas widgets (colored circle + initial letter).
  - Animated spinner in the status bar during S3 operations.
  - Card-style settings, compose, and detail dialogs.
  - Placeholder text in all input fields.
  - Media open button distinguishes images 🖼 vs files 📎.
  - Portable `ArvanShare.exe` via PyInstaller (`build_exe.bat`).
  - `ArvanShare.bat` launcher auto-detects and prefers the portable EXE.

- **Python CLI** (`arvanshare.py`)
  - Reference implementation for all S3 operations.
  - Shared key layout with the Android app (kept in sync).
  - Offline test suite with moto mock S3 (`test_mock.py`).

- **Repository**
  - Root `README.md` with quick-start guides for all three surfaces.
  - `CHANGELOG.md` (this file).
  - `keystore/README.md` with GitHub Actions secrets setup instructions.
  - GitHub Actions release workflow (`release.yml`):
    - Builds signed Android APK on `v*` tag push.
    - Runs Python tests then builds Windows EXE via PyInstaller.
    - Creates a GitHub Release with both assets attached.
  - Tightened `.gitignore` (no secrets, no build artefacts).

### Architecture
- **Serverless / backend-less**: all data lives as JSON + marker files in one
  shared ArvanCloud Object Storage bucket (S3-compatible).
- **Race-safe likes**: each like is an empty `like_<user>.txt` marker file;
  creating or deleting it is an atomic S3 operation.
- **Offline-first Android**: Room caches post metadata; feed renders instantly
  on launch, S3 sync happens in background.
