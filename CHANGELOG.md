# Changelog

All notable changes to ArvanShare are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.0.0-beta.3] — 2026-10-17

### Added
- **🎙 Voice notes on every client** (breaking-free new feature, full Android ↔ Windows parity)
  - Android: "Record a voice note" in the composer (runtime `RECORD_AUDIO`
    permission flow), recording indicator with an elapsed timer, and an inline
    `VoicePlayer` (play/pause + progress + durations) in the feed and post
    detail. Recorded as compact AAC/M4A via `MediaRecorder`.
  - Windows: "🎙 Record voice" in the compose dialog, recording via
    `sounddevice` (no NumPy needed), stored as a 16 kHz mono WAV; WAV voice
    notes play **inline** through `winsound`, other formats (e.g. M4A from
    Android) open in the system player.
  - Both use `media_type="audio"`; old clients still render such posts as a
    downloadable attachment chip (fully backward compatible).
- **Inline image previews** in the Windows post-detail dialog (Pillow,
  graceful no-op when unavailable).
- CLI/desktop now label audio posts as 🎙 in the feed, and Android shows 🎙️
  instead of 📎 for voice notes.
- Display-name sanitization is now shown as a hint during setup on Android.

### Changed
- **Incremental feed sync** (Android, spec section 6.2): refresh no longer
  re-downloads *every* post JSON. One keys-only listing diffs the remote feed
  against the Room cache; only unseen posts are fetched and posts deleted
  remotely are evicted locally. Likes/comments listings are paginated too
  (previously silently truncated at 1000 items).
- New brand logo (`assets/logo.svg` + `docs/assets/logo.svg`): a bold gradient
  speech bubble (indigo → brand teal) carrying the white share-nodes glyph,
  plus a matching Android launcher icon (adaptive foreground + themed
  monochrome layers on a dark navy tile) and setup-screen brand mark.
- `media_mime` now understands `m4a/aac/ogg/flac` on both platforms.

### Fixed
- **Windows: display name ("name") was never written to `config.ini`**, so the
  app forced re-setup after every restart and posting as the wrong/empty name.
- **Cross-platform key mismatch:** Python sanitized filenames with
  Unicode-aware `str.isalnum()` while Android used an ASCII-only regex — the
  same attachment produced *different S3 keys* per platform (violating the
  shared contract). Both now use the exact same ASCII-only rule for filenames
  *and* user names (post ids, like markers, comment files).
- Android: the like-heart spring animation stayed stuck at 1.3× scale after a
  like; it is now a one-shot pop that always settles back to 1×.
- Android: a failed comment send used to erase the draft — the text is now
  only cleared on success.
- Android: picking a file from a provider with a null input stream produced a
  zero-byte attachment; that case is rejected now.
- Android: the media cache grew without bound; it now trims to 128 MB
  (oldest files first).
- Windows: avatar colors reshuffled on every app restart (per-process hash
  salting) — now stable via `zlib.crc32`.
- Windows: version label in the status bar is now derived from
  `arvanshare.__version__` instead of a hardcoded string.
- Windows: attachments with identical filenames overwrote each other in the
  temp directory — downloads now use unique temp names.

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
