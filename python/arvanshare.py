#!/usr/bin/env python3
"""ArvanShare - reference CLI for a private, serverless social feed on ArvanCloud Object Storage (S3-compatible).

All data lives as JSON/marker files in one shared bucket - no database, no backend.
Structure (see private_social_media_specs.md):

    /
    ├── posts/
    │   ├── 20260806_120000_ali_post.json     # post metadata + text
    │   └── 20260806_120000_ali_image.jpg     # optional media
    └── comments/
        └── 20260806_120000_ali/              # one folder per post_id
            ├── like_reza.txt                 # empty marker = like (race-safe)
            └── 20260806_120530_reza.json     # a comment

Usage:
    python arvanshare.py init-structure
    python arvanshare.py upload-post --text "Hello!" --image photo.jpg --as Ali
    python arvanshare.py list-posts
    python arvanshare.py get-post <post_id>
    python arvanshare.py upload-comment <post_id> --text "Nice!" --as Reza
    python arvanshare.py toggle-like <post_id> --as Sara
"""

import argparse
import configparser
import json
import os
import sys
import time
from datetime import datetime

import boto3
from botocore.exceptions import ClientError

POST_PREFIX = "posts/"
COMMENTS_PREFIX = "comments/"
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
    """Timestamp with milliseconds so rapid posts by the same user never collide.

    Format: YYYYMMDD_HHMMSSffffff - still lexicographically sortable by time,
    which is what the app uses to fetch "only new posts" (spec section 5).
    """
    return datetime.now().strftime("%Y%m%d_%H%M%S%f")


def make_post_id(user: str) -> str:
    return f"{now_str()}_{user}"


def post_json_key(post_id: str) -> str:
    return f"{POST_PREFIX}{post_id}_post.json"


def post_media_key(post_id: str, filename: str) -> str:
    """Media key keeps the original filename so any file type round-trips.

    Kept in sync with the Android app's Keys.postMediaKey - same sanitization.
    """
    safe = "".join(c if (c.isalnum() or c in "._-") else "_" for c in filename)
    safe = safe or "file"
    return f"{POST_PREFIX}{post_id}_{safe}"


def comments_folder(post_id: str) -> str:
    return f"{COMMENTS_PREFIX}{post_id}/"


def like_key(post_id: str, user: str) -> str:
    return f"{comments_folder(post_id)}like_{user}.txt"


def comment_key(post_id: str, user: str) -> str:
    return f"{comments_folder(post_id)}{now_str()}_{user}.json"


# --------------------------------------------------------------------------- #
# Core operations
# --------------------------------------------------------------------------- #

def _is_404(exc: ClientError) -> bool:
    return exc.response["ResponseMetadata"]["HTTPStatusCode"] == 404


def init_structure(client, bucket: str) -> None:
    """Create the posts/ and comments/ folders as zero-byte placeholder objects."""
    for key in PLACEHOLDER_KEYS:
        try:
            client.head_object(Bucket=bucket, Key=key)
        except client.exceptions.ClientError as exc:
            if _is_404(exc):
                client.put_object(Bucket=bucket, Key=key, Body=b"")
                print(f"created {key}")
            else:
                raise
        else:
            print(f"exists  {key}")


def upload_post(client, bucket: str, user: str, text: str, media_path: str | None) -> str:
    """Upload optional media (any file type) first, then the post JSON.

    Returns the post_id. The post JSON carries media_url, media_type (image|file),
    media_name (original filename) and media_mime for the clients to render.
    """
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
        key = post_media_key(post_id, media_name)
        client.upload_file(media_path, bucket, key, ExtraArgs={"ContentType": media_mime})
        media_url = key
        media_type = "image" if media_mime.startswith("image/") else "file"

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
        # NOTE: no "likes" field on purpose - likes live in like_*.txt markers
        # (see spec section 6.1, race-safe alternative).
    }
    client.put_object(
        Bucket=bucket,
        Key=post_json_key(post_id),
        Body=json.dumps(post, ensure_ascii=False, indent=2).encode("utf-8"),
        ContentType="application/json",
    )
    print(f"uploaded post {post_id}")
    return post_id


def guess_mime(name: str) -> str:
    """Best-effort MIME guess from the file extension (kept in sync with Android)."""
    ext = name.rsplit(".", 1)[-1].lower() if "." in name else ""
    return {
        "jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png",
        "gif": "image/gif", "webp": "image/webp",
        "mp4": "video/mp4", "mov": "video/quicktime",
        "mp3": "audio/mpeg", "wav": "audio/wav",
        "pdf": "application/pdf",
        "txt": "text/plain", "md": "text/markdown", "csv": "text/csv",
        "zip": "application/zip", "gz": "application/gzip",
        "doc": "application/msword", "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls": "application/vnd.ms-excel", "xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt": "application/vnd.ms-powerpoint", "pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    }.get(ext, "application/octet-stream")


def _is_post_key(key: str) -> bool:
    return key.startswith(POST_PREFIX) and key.endswith("_post.json")


def _newest_first(post: dict) -> tuple:
    return (post.get("timestamp", 0), post.get("post_id", ""))


def list_posts(client, bucket: str, limit: int | None = None) -> list[dict]:
    """List posts, newest first. Uses ContinuationToken pagination (spec 6.2)."""
    posts = []
    token = None
    page_size = 100
    while True:
        kwargs = {"Bucket": bucket, "Prefix": POST_PREFIX, "MaxKeys": page_size}
        if token:
            kwargs["ContinuationToken"] = token
        response = client.list_objects_v2(**kwargs)
        for obj in response.get("Contents", []):
            key = obj["Key"]
            if _is_post_key(key):
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


def fetch_post(client, bucket: str, post_id: str) -> dict:
    body = client.get_object(Bucket=bucket, Key=post_json_key(post_id))["Body"].read()
    return json.loads(body.decode("utf-8"))


def _folder_keys(client, bucket: str, folder: str) -> list[str]:
    """Basenames of every object under a folder (likes, comments)."""
    response = client.list_objects_v2(Bucket=bucket, Prefix=folder)
    return [obj["Key"][len(folder):] for obj in response.get("Contents", [])]


def post_likes(client, bucket: str, post_id: str) -> list[str]:
    """Likes come from empty like_<user>.txt marker files (race-safe by design)."""
    folder = comments_folder(post_id)
    return [
        name[len("like_"):-len(".txt")]
        for name in _folder_keys(client, bucket, folder)
        if name.startswith("like_") and name.endswith(".txt")
    ]


def post_comments(client, bucket: str, post_id: str) -> list[dict]:
    folder = comments_folder(post_id)
    comments = []
    for name in _folder_keys(client, bucket, folder):
        if name.startswith("like_") or name.endswith(".txt"):
            continue
        key = f"{folder}{name}"
        body = client.get_object(Bucket=bucket, Key=key)["Body"].read()
        comments.append(json.loads(body.decode("utf-8")))
    return sorted(comments, key=lambda c: c.get("timestamp", 0))


def upload_comment(client, bucket: str, post_id: str, user: str, text: str) -> None:
    comment = {
        "post_id": post_id,
        "author": user,
        "text": text,
        "timestamp": int(time.time()),
    }
    client.put_object(
        Bucket=bucket,
        Key=comment_key(post_id, user),
        Body=json.dumps(comment, ensure_ascii=False, indent=2).encode("utf-8"),
        ContentType="application/json",
    )
    print(f"commented on {post_id} as {user}")


def toggle_like(client, bucket: str, post_id: str, user: str) -> bool:
    """Add the like if missing, remove it if present. Returns True if now liked."""
    key = like_key(post_id, user)
    try:
        client.head_object(Bucket=bucket, Key=key)
        client.delete_object(Bucket=bucket, Key=key)
        print(f"unliked {post_id} as {user}")
        return False
    except client.exceptions.ClientError as exc:
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

    p = sub.add_parser("upload-post", help="publish a text post, optionally with a file")
    p.add_argument("--text", required=True)
    p.add_argument("--file", help="path to a file to attach (any type: image, pdf, zip, ...)")
    p.add_argument("--as", dest="user", required=True, help="author name")

    p = sub.add_parser("list-posts", help="show the feed, newest first")
    p.add_argument("--limit", type=int, default=None)

    p = sub.add_parser("get-post", help="show one post with its likes and comments")
    p.add_argument("post_id")

    p = sub.add_parser("upload-comment", help="comment on a post")
    p.add_argument("post_id")
    p.add_argument("--text", required=True)
    p.add_argument("--as", dest="user", required=True)

    p = sub.add_parser("toggle-like", help="like/unlike a post")
    p.add_argument("post_id")
    p.add_argument("--as", dest="user", required=True)

    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    cfg = load_config(args.config)
    client = build_client(cfg)
    bucket = bucket_name(cfg)

    if args.command == "init-structure":
        init_structure(client, bucket)
    elif args.command == "upload-post":
        upload_post(client, bucket, args.user, args.text, args.file)
    elif args.command == "list-posts":
        for post in list_posts(client, bucket, args.limit):
            print(f"[{post['post_id']}] {post['author']}: {post['text']}")
    elif args.command == "get-post":
        post = fetch_post(client, bucket, args.post_id)
        print(json.dumps(post, ensure_ascii=False, indent=2))
        likes = post_likes(client, bucket, args.post_id)
        if likes:
            print("likes:", ", ".join(likes))
        for comment in post_comments(client, bucket, args.post_id):
            print(f"  {comment['author']}: {comment['text']}")
    elif args.command == "upload-comment":
        upload_comment(client, bucket, args.post_id, args.user, args.text)
    elif args.command == "toggle-like":
        toggle_like(client, bucket, args.post_id, args.user)
    return 0


if __name__ == "__main__":
    sys.exit(main())
