# ArvanShare — Python reference CLI

A small CLI that talks to your shared ArvanCloud Object Storage bucket. It mirrors
exactly the same file layout the Android app uses, so you can verify the whole
data model before/without building the app.

```
/
├── posts/
│   ├── 20260806_120000_ali_post.json     # post metadata + text
│   └── 20260806_120000_ali_image.jpg     # optional media
└── comments/
    └── 20260806_120000_ali/              # one folder per post_id
        ├── like_reza.txt                 # empty marker = like (race-safe)
        └── 20260806_120530_reza.json     # a comment
```

## 1. One-time ArvanCloud setup (gavm 0 of the spec)

1. Go to [panel.arvancloud.ir](https://panel.arvancloud.ir) and open **Object Storage**.
2. **Create a bucket** (e.g. `my-family-share`).
   - Pick a region close to you. The region code appears in the S3 endpoint.
   - Note the **S3 Endpoint** of the bucket, e.g. `https://s3.ir-thr-at1.arvanstorage.ir`.
3. In Object Storage → **API Keys**, generate a key pair:
   - Copy **Access Key** and **Secret Key** (secret is shown only once — save it).
   - Attach the key to the bucket you created (read/write).
4. Copy `config.example.ini` to `config.ini` and fill in endpoint, region, bucket, keys.

> Keep `config.ini` private — it is gitignored. Never commit real keys.

## 2. Setup

```bash
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt   # boto3, moto, pytest
```

## 3. Usage

### Desktop app (Windows/macOS/Linux)

```bash
.venv/Scripts/python.exe desktop.py
```

- First run opens the **Settings** dialog: enter your display name, S3 endpoint,
  bucket, and keys (saved to `config.ini` next to the script — keep it private).
- **New post** publishes text (+ optional image). **Refresh** reloads the feed.
- Double-click a post to open it: like/unlike and comment there.
- Images open in your default viewer (downloaded on demand through S3).

### Command line

```bash
.venv/Scripts/python.exe arvanshare.py init-structure
.venv/Scripts/python.exe arvanshare.py upload-post --text "Hello everyone!" --as Ali
.venv/Scripts/python.exe arvanshare.py upload-post --text "Look at this" --image photo.jpg --as Sara
.venv/Scripts/python.exe arvanshare.py list-posts
.venv/Scripts/python.exe arvanshare.py get-post 20260806_120000_ali
.venv/Scripts/python.exe arvanshare.py toggle-like 20260806_120000_ali --as Reza
.venv/Scripts/python.exe arvanshare.py upload-comment 20260806_120000_ali --text "Nice!" --as Reza
```

## 4. Offline tests (no account needed)

Runs the whole data model against moto's in-memory S3:

```bash
.venv/Scripts/python.exe -m pytest test_mock.py -q
```

## Naming contract (keep in sync with the Android app)

- Post id: `YYYYMMDD_HHMMSSffffff_<user>` (millisecond precision → no collisions).
- Post JSON: `posts/<post_id>_post.json`
- Media: `posts/<post_id>_image.<ext>`
- Likes: `comments/<post_id>/like_<user>.txt` (empty file — creating/deleting it
  is an atomic like/unlike, avoids the race described in spec section 6.1)
- Comments: `comments/<post_id>/<timestamp>_<user>.json`
