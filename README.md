# ArvanShare

ArvanShare is a **serverless, private social feed** for a small circle of friends, family, or a team. It has no backend server or database — instead, **all data (posts, comments, likes, and media) is stored directly on an ArvanCloud Object Storage bucket** (S3-compatible).

Clients connect directly to the bucket using an identical, race-condition-safe file layout.

## 📱 Android App (Kotlin / Jetpack Compose)

A modern, offline-first native Android app.
It caches post metadata in a local Room database so the feed loads instantly when opened, and downloads full-res media on demand via Coil.

**To run:**
1. Open the project in Android Studio.
2. Build and run the `app` module.
3. On first launch, enter your display name and the ArvanCloud bucket details.

*(Or download the `.apk` from the GitHub Releases page).*

---

## 💻 Windows Desktop App (Python / Tkinter)

A fully-featured desktop client with a dark-mode UI, card-style feed, and attachment support.

**To run (Portable):**
1. Download `ArvanShare-*.exe` from the GitHub Releases page.
2. Double-click to run — no Python installation needed.
3. Settings are saved locally alongside the `.exe` (or in `%LOCALAPPDATA%`).

**To run (From source):**
1. Ensure you have Python 3.10+ installed.
2. Double-click `python\ArvanShare.bat`.
3. It will guide you to create the virtual environment and install dependencies (`boto3`).

*(You can also manually run `.venv\Scripts\python.exe desktop.py`)*

---

## 🛠️ Command Line Interface (CLI)

A reference CLI tool for managing the feed or automating posts.

**Setup:**
```bash
cd python
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
copy config.example.ini config.ini
# Edit config.ini with your details
```

**Usage:**
```bash
.venv\Scripts\python arvanshare.py init-structure
.venv\Scripts\python arvanshare.py upload-post --text "Hello everyone!" --as Ali
.venv\Scripts\python arvanshare.py list-posts
.venv\Scripts\python arvanshare.py get-post <post_id>
```

---

## ☁️ Setting up the ArvanCloud Bucket (Serverless backend)

To use ArvanShare, you need one shared bucket where all the data lives.
You will need 4 pieces of information: **Bucket Name**, **S3 Endpoint**, **Access Key**, and **Secret Key**. Follow these steps to get them:

1. **Create a Bucket:**
   - Go to [panel.arvancloud.ir](https://panel.arvancloud.ir) -> **Object Storage**.
   - Click **New Bucket**. Choose a unique name (e.g. `my-family-share`) and set the access level to **Private**. This is your **Bucket Name**.

2. **Get the S3 Endpoint:**
   - In the Object Storage dashboard, you will see the **S3 Endpoint** URL corresponding to your region (e.g., `https://s3.ir-thr-at1.arvanstorage.ir`). This is your **Endpoint**.

3. **Generate API Keys (Access & Secret Key):**
   - Go to **API Keys** in the Object Storage menu.
   - Click **New Key**. Give it a name and ensure the access level is **Read & Write**.
   - **Important:** Attach this new key to the bucket you created in step 1.
   - Once created, you will be shown the **Access Key** and **Secret Key**. *(Note: The Secret Key is only shown once, so save it somewhere safe)*.

Give these 4 pieces of info (Endpoint, Bucket, Access Key, Secret Key) to everyone in your circle. They will enter them into the app on first launch.

---

## 🏗️ Architecture & Data Model

The entire social network is mapped to S3 keys.

```text
/
├── posts/
│   ├── 20260806_120000000_ali_post.json    # JSON metadata (text, author, time)
│   └── 20260806_120000000_ali_image.jpg    # Media attachment
└── comments/
    └── 20260806_120000000_ali/             # Folder for this post's interactions
        ├── like_reza.txt                   # Empty marker file = one like
        └── 20260806_120530_reza.json       # Comment JSON
```

### Race-condition safe
Because there is no central database to handle concurrent writes, the data model avoids modifying shared files.
- **Posts**: Each post is a new, unique file.
- **Comments**: Each comment is a new, unique file inside the post's folder.
- **Likes**: Instead of updating a counter inside `post.json`, a like is simply an empty `like_<username>.txt` file. S3 `PutObject` and `DeleteObject` are atomic, so multiple users liking at the exact same time will never corrupt data or overwrite each other's likes.

---

## 🔐 Releases & GitHub Actions

When a new version tag (e.g. `v1.0.0-beta.1`) is pushed, GitHub Actions automatically:
1. Builds and signs the Android APK (using the `release.jks` keystore passed via Secrets).
2. Builds the standalone Windows `.exe` using PyInstaller.
3. Creates a GitHub Release with both assets attached.

See `keystore/README.md` for instructions on setting up the signing secrets.
