#!/usr/bin/env python3
"""ArvanShare - reference CLI for a private, serverless social feed on ArvanCloud Object Storage (S3-compatible).

All data lives as JSON/marker files in one shared bucket - no database, no backend.
Supports default feed and private rooms with optional simple password protection.

Structure (see private_social_media_specs.md):

    /
    ├── posts/
    │   ├── 20260806_120000_ali_post.json     # post metadata + text
    │   └── 20260806_120000_ali_image.jpg     # optional media
    ├── comments/
    │   └── 20260806_120000_ali/              # one folder per post_id
    │       ├── like_reza.txt                 # empty marker = like (race-safe)
    │       └── 20260806_120530_reza.json     # a comment
    └── rooms/
        └── family/
            ├── room.json                     # room metadata and password hash
            ├── posts/
            └── comments/

Usage:
    python arvanshare.py init-structure
    python arvanshare.py list-rooms
    python arvanshare.py create-room family --password 1234 --as Ali
    python arvanshare.py upload-post --text "Hello!" --as Ali [--room family]
    python arvanshare.py list-posts [--room family]
    python arvanshare.py get-post <post_id> [--room family]
    python arvanshare.py upload-comment <post_id> --text "Nice!" --as Reza [--room family]
    python arvanshare.py toggle-like <post_id> --as Sara [--room family]
"""

import argparse
import configparser
import hashlib
import json
import os
import re
import sys
import time
from datetime import datetime

import boto3
from botocore.exceptions import ClientError

__version__ = "1.0.0"

POST_PREFIX = "posts/"
COMMENTS_PREFIX = "comments/"
ROOMS_PREFIX = "rooms/"
PLACEHOLDER_KEYS = (POST_PREFIX, COMMENTS_PREFIX)  # zero-byte "folder" markers


# --------------------------------------------------------------------------- #
# Config
# --------------------------------------------------------------------------- #

def load_config(path: str | None = None) -> configparser.ConfigParser:
    cfg = configparser.ConfigParser()
    candidates = [path] if path else [
        os.path.join(os.path.dirname(__file__), "config.ini"),
        os.path.join(os.path.dirname(__file__), "config.example.ini"),
    ]
    for candidate in candidates:
        if os.path.exists(candidate):
            cfg.read(candidate)
            return cfg
    sys.exit("Config file not found. Copy config.example.ini to config.ini and fill it in.")


def build_client(cfg: configparser.ConfigParser):
    section = cfg["arvan"]
    return boto3.client(
        "s3",
        endpoint_url=section.get("endpoint"),
        region_name=section.get("region"),
        aws_access_key_id=section.get("access_key"),
        aws_secret_access_key=section.get("secret_key"),
    )


def bucket_name(cfg: configparser.ConfigParser) -> str:
    return cfg["arvan"].get("bucket")


# --------------------------------------------------------------------------- #
# Naming helpers (shared with the Android app - keep in sync)
# --------------------------------------------------------------------------- #

def now_str() -> str:
    """Timestamp with milliseconds so rapid posts by the same user never collide."""
    return datetime.now().strftime("%Y%m%d_%H%M%S%f")


# Keep names/keys S3-safe and identical on every platform. MUST be
# character-for-character the same rule as Android's Keys.sanitizeFilename /
# Keys.sanitizeName: ASCII letters, digits, dot, underscore, dash only.
_SAFE_RE = re.compile(r"[^A-Za-z0-9._-]")


def sanitize_segment(value: str, fallback: str) -> str:
    """Sanitize a filename/username/room segment so it is a safe S3 key fragment."""
    base = _SAFE_RE.sub("_", (value or "").strip())
    return base or fallback


def sanitize_filename(filename: str) -> str:
    return sanitize_segment(os.path.basename(filename.replace("\\", "/")), "file")


def sanitize_user(user: str) -> str:
    return sanitize_segment(user, "user")


def sanitize_room(room: str) -> str:
    return sanitize_segment(room, "general")


def is_default_room(room: str | None) -> bool:
    return not room or room.strip().lower() in ("default", "main")


def post_prefix(room: str | None = None) -> str:
    return POST_PREFIX if is_default_room(room) else f"{ROOMS_PREFIX}{sanitize_room(room)}/{POST_PREFIX}"


def comments_prefix(room: str | None = None) -> str:
    return COMMENTS_PREFIX if is_default_room(room) else f"{ROOMS_PREFIX}{sanitize_room(room)}/{COMMENTS_PREFIX}"


def room_info_key(room: str) -> str:
    return f"{ROOMS_PREFIX}{sanitize_room(room)}/room.json"


def hash_password(password: str) -> str:
    """SHA-256 hex string for room password protection."""
    if not password or not password.strip():
        return ""
    return hashlib.sha256(password.strip().encode("utf-8")).hexdigest()


def make_post_id(user: str) -> str:
    return f"{now_str()}_{sanitize_user(user)}"


def post_json_key(post_id: str, room: str | None = None) -> str:
    return f"{post_prefix(room)}{post_id}_post.json"


def post_media_key(post_id: str, filename: str, room: str | None = None) -> str:
    """Media key keeps the original filename so any file type round-trips."""
    return f"{post_prefix(room)}{post_id}_{sanitize_filename(filename)}"


def comments_folder(post_id: str, room: str | None = None) -> str:
    return f"{comments_prefix(room)}{post_id}/"


def like_key(post_id: str, user: str, room: str | None = None) -> str:
    return f"{comments_folder(post_id, room)}like_{sanitize_user(user)}.txt"


def comment_key(post_id: str, user: str, room: str | None = None) -> str:
    return f"{comments_folder(post_id, room)}{now_str()}_{sanitize_user(user)}.json"


# --------------------------------------------------------------------------- #
# Core operations
# --------------------------------------------------------------------------- #

def _is_404(exc: Exception) -> bool:
    resp = getattr(exc, "response", {})
    if isinstance(resp, dict):
        if resp.get("ResponseMetadata", {}).get("HTTPStatusCode") == 404:
            return True
        if resp.get("Error", {}).get("Code") in ("404", "NoSuchKey"):
            return True
    return False


def init_structure(client, bucket: str) -> None:
    """Create the posts/ and comments/ folders as zero-byte placeholder objects."""
    for key in PLACEHOLDER_KEYS:
        try:
            client.head_object(Bucket=bucket, Key=key)
        except Exception as exc:
            if _is_404(exc):
                client.put_object(Bucket=bucket, Key=key, Body=b"")
                print(f"created {key}")
            else:
                raise
        else:
            print(f"exists  {key}")


def create_or_verify_room(
    client,
    bucket: str,
    room: str,
    password: str = "",
    user: str = "",
) -> dict:
    """Verify password for an existing room, or create a new room with an optional password."""
    if is_default_room(room):
        return {"name": "default", "has_password": False}

    key = room_info_key(room)
    try:
        obj = client.get_object(Bucket=bucket, Key=key)
        info = json.loads(obj["Body"].read().decode("utf-8"))
        if info.get("has_password"):
            expected = info.get("password_hash") or ""
            entered = hash_password(password)
            if expected and entered != expected:
                raise ValueError(f"Incorrect password for room '{room}'")
        return info
    except client.exceptions.ClientError as exc:
        if not _is_404(exc):
            raise
        # Create the room
        has_pwd = bool(password and password.strip())
        info = {
            "name": sanitize_room(room),
            "has_password": has_pwd,
            "password_hash": hash_password(password) if has_pwd else None,
            "created_at": int(time.time()),
            "created_by": user or "user",
        }
        client.put_object(
            Bucket=bucket,
            Key=key,
            Body=json.dumps(info, ensure_ascii=False, indent=2).encode("utf-8"),
            ContentType="application/json",
        )
        print(f"created room '{sanitize_room(room)}' (password protected: {has_pwd})")
        return info


def list_rooms(client, bucket: str) -> list[dict]:
    """List all rooms in the bucket."""
    rooms = []
    token = None
    while True:
        kwargs = {"Bucket": bucket, "Prefix": ROOMS_PREFIX, "MaxKeys": 100}
        if token:
            kwargs["ContinuationToken"] = token
        response = client.list_objects_v2(**kwargs)
        for obj in response.get("Contents", []):
            key = obj["Key"]
            if key.endswith("/room.json"):
                try:
                    body = client.get_object(Bucket=bucket, Key=key)["Body"].read()
                    rooms.append(json.loads(body.decode("utf-8")))
                except Exception:
                    pass
        if response.get("IsTruncated"):
            token = response.get("NextContinuationToken")
        else:
            break
    return sorted(rooms, key=lambda r: r.get("name", ""))


def upload_post(
    client,
    bucket: str,
    user: str,
    text: str,
    media_path: str | None = None,
    room: str | None = None,
) -> str:
    """Upload optional media first, then post JSON, optionally in a room."""
    post_id = make_post_id(user)
    media_url = None
    media_type = None
    media_name = None
    media_mime = None
    if media_path:
        if not os.path.exists(media_path):
            sys.exit(f"File not found: {media_path}")
        media_name = os.path.basename(media_path)
        media_mime = guess_mime(media_name)
        key = post_media_key(post_id, media_name, room)
        client.upload_file(media_path, bucket, key, ExtraArgs={"ContentType": media_mime})
        media_url = key
        media_type = media_kind(media_mime)

    post = {
        "post_id": post_id,
        "author": user,
        "avatar_url": None,
        "text": text,
        "media_url": media_url,
        "media_type": media_type,
        "media_name": media_name,
        "media_mime": media_mime,
        "timestamp": int(time.time()),
        "room": sanitize_room(room) if not is_default_room(room) else "",
    }
    client.put_object(
        Bucket=bucket,
        Key=post_json_key(post_id, room),
        Body=json.dumps(post, ensure_ascii=False, indent=2).encode("utf-8"),
        ContentType="application/json",
    )
    print(f"uploaded post {post_id}" + (f" in room {room}" if room else ""))
    return post_id


def media_kind(mime: str) -> str:
    """Render kind for a MIME type: image | audio | file."""
    if mime.startswith("image/"):
        return "image"
    if mime.startswith("audio/"):
        return "audio"
    return "file"


def guess_mime(name: str) -> str:
    """Best-effort MIME guess from the file extension."""
    ext = name.rsplit(".", 1)[-1].lower() if "." in name else ""
    return {
        "jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png",
        "gif": "image/gif", "webp": "image/webp",
        "mp4": "video/mp4", "mov": "video/quicktime",
        "mp3": "audio/mpeg", "wav": "audio/wav",
        "m4a": "audio/mp4", "aac": "audio/aac", "ogg": "audio/ogg", "oga": "audio/ogg",
        "flac": "audio/flac",
        "pdf": "application/pdf",
        "txt": "text/plain", "md": "text/markdown", "csv": "text/csv",
        "zip": "application/zip", "gz": "application/gzip",
        "doc": "application/msword", "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls": "application/vnd.ms-excel", "xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt": "application/vnd.ms-powerpoint", "pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    }.get(ext, "application/octet-stream")


def _is_post_key(key: str, room: str | None = None) -> bool:
    prefix = post_prefix(room)
    return key.startswith(prefix) and key.endswith("_post.json")


def _newest_first(post: dict) -> tuple:
    return (post.get("timestamp", 0), post.get("post_id", ""))


def list_posts(
    client,
    bucket: str,
    limit: int | None = None,
    room: str | None = None,
) -> list[dict]:
    """List posts, newest first, scoped to a room or default feed."""
    posts = []
    token = None
    page_size = 100
    prefix = post_prefix(room)
    while True:
        kwargs = {"Bucket": bucket, "Prefix": prefix, "MaxKeys": page_size}
        if token:
            kwargs["ContinuationToken"] = token
        response = client.list_objects_v2(**kwargs)
        for obj in response.get("Contents", []):
            key = obj["Key"]
            if _is_post_key(key, room):
                body = client.get_object(Bucket=bucket, Key=key)["Body"].read()
                post = json.loads(body.decode("utf-8"))
                posts.append(post)
                if limit and len(posts) >= limit:
                    return sorted(posts, key=_newest_first, reverse=True)[:limit]
        if response.get("IsTruncated"):
            token = response.get("NextContinuationToken")
        else:
            break
    return sorted(posts, key=_newest_first, reverse=True)


def fetch_post(client, bucket: str, post_id: str, room: str | None = None) -> dict:
    body = client.get_object(Bucket=bucket, Key=post_json_key(post_id, room))["Body"].read()
    return json.loads(body.decode("utf-8"))


def _folder_keys(client, bucket: str, folder: str) -> list[str]:
    """Basenames of every object under a folder (likes, comments)."""
    response = client.list_objects_v2(Bucket=bucket, Prefix=folder)
    return [obj["Key"][len(folder):] for obj in response.get("Contents", [])]


def post_likes(client, bucket: str, post_id: str, room: str | None = None) -> list[str]:
    folder = comments_folder(post_id, room)
    return [
        name[len("like_"):-len(".txt")]
        for name in _folder_keys(client, bucket, folder)
        if name.startswith("like_") and name.endswith(".txt")
    ]


def post_comments(client, bucket: str, post_id: str, room: str | None = None) -> list[dict]:
    folder = comments_folder(post_id, room)
    comments = []
    for name in _folder_keys(client, bucket, folder):
        if name.startswith("like_") or name.endswith(".txt"):
            continue
        key = f"{folder}{name}"
        body = client.get_object(Bucket=bucket, Key=key)["Body"].read()
        comments.append(json.loads(body.decode("utf-8")))
    return sorted(comments, key=lambda c: c.get("timestamp", 0))


def upload_comment(
    client,
    bucket: str,
    post_id: str,
    user: str,
    text: str,
    room: str | None = None,
) -> None:
    comment = {
        "post_id": post_id,
        "author": user,
        "text": text,
        "timestamp": int(time.time()),
    }
    client.put_object(
        Bucket=bucket,
        Key=comment_key(post_id, user, room),
        Body=json.dumps(comment, ensure_ascii=False, indent=2).encode("utf-8"),
        ContentType="application/json",
    )
    print(f"commented on {post_id} as {user}")


def toggle_like(
    client,
    bucket: str,
    post_id: str,
    user: str,
    room: str | None = None,
) -> bool:
    """Add the like if missing, remove it if present. Returns True if now liked."""
    key = like_key(post_id, user, room)
    try:
        client.head_object(Bucket=bucket, Key=key)
        client.delete_object(Bucket=bucket, Key=key)
        print(f"unliked {post_id} as {user}")
        return False
    except Exception as exc:
        if _is_404(exc):
            client.put_object(Bucket=bucket, Key=key, Body=b"")
            print(f"liked {post_id} as {user}")
            return True
        raise


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="ArvanShare S3-backed social feed CLI")
    parser.add_argument("-c", "--config", help="path to config ini (default: config.ini)")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("init-structure", help="create posts/ and comments/ folders")

    sub.add_parser("list-rooms", help="list all rooms in the bucket")

    cr = sub.add_parser("create-room", help="create or enter a room")
    cr.add_argument("room", help="room name")
    cr.add_argument("--password", default="", help="optional simple password")
    cr.add_argument("--as", dest="user", default="user", help="creator name")

    p = sub.add_parser("upload-post", help="publish a text post, optionally with a file")
    p.add_argument("--text", required=True)
    p.add_argument("--file", help="path to a file to attach (any type: image, pdf, zip, ...)")
    p.add_argument("--as", dest="user", required=True, help="author name")
    p.add_argument("--room", default=None, help="target room name (default: main feed)")

    p = sub.add_parser("list-posts", help="show the feed, newest first")
    p.add_argument("--limit", type=int, default=None)
    p.add_argument("--room", default=None, help="target room name")

    p = sub.add_parser("get-post", help="show one post with its likes and comments")
    p.add_argument("post_id")
    p.add_argument("--room", default=None, help="target room name")

    p = sub.add_parser("upload-comment", help="comment on a post")
    p.add_argument("post_id")
    p.add_argument("--text", required=True)
    p.add_argument("--as", dest="user", required=True)
    p.add_argument("--room", default=None, help="target room name")

    p = sub.add_parser("toggle-like", help="like/unlike a post")
    p.add_argument("post_id")
    p.add_argument("--as", dest="user", required=True)
    p.add_argument("--room", default=None, help="target room name")

    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    cfg = load_config(args.config)
    client = build_client(cfg)
    bucket = bucket_name(cfg)

    if args.command == "init-structure":
        init_structure(client, bucket)
    elif args.command == "list-rooms":
        for r in list_rooms(client, bucket):
            pwd = " [password-protected]" if r.get("has_password") else ""
            print(f"#{r['name']}{pwd}")
    elif args.command == "create-room":
        create_or_verify_room(client, bucket, args.room, args.password, args.user)
    elif args.command == "upload-post":
        upload_post(client, bucket, args.user, args.text, args.file, args.room)
    elif args.command == "list-posts":
        for post in list_posts(client, bucket, args.limit, args.room):
            print(f"[{post['post_id']}] {post['author']}: {post['text']}")
    elif args.command == "get-post":
        post = fetch_post(client, bucket, args.post_id, args.room)
        print(json.dumps(post, ensure_ascii=False, indent=2))
        likes = post_likes(client, bucket, args.post_id, args.room)
        if likes:
            print("likes:", ", ".join(likes))
        for comment in post_comments(client, bucket, args.post_id, args.room):
            print(f"  {comment['author']}: {comment['text']}")
    elif args.command == "upload-comment":
        upload_comment(client, bucket, args.post_id, args.user, args.text, args.room)
    elif args.command == "toggle-like":
        toggle_like(client, bucket, args.post_id, args.user, args.room)
    return 0


if __name__ == "__main__":
    sys.exit(main())
