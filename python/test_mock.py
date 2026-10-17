"""Offline tests for the ArvanShare data model using moto's mock S3.

These run against an in-memory S3 so no real ArvanCloud account is needed.
The same key layout is used by the Android app - keep naming helpers in sync.
"""

import configparser
import json

import boto3
import pytest
from moto import mock_aws

from arvanshare import (
    build_client,
    bucket_name,
    comments_folder,
    guess_mime,
    init_structure,
    list_posts,
    make_post_id,
    post_comments,
    post_json_key,
    post_likes,
    post_media_key,
    sanitize_filename,
    sanitize_user,
    toggle_like,
    upload_comment,
    upload_post,
)


@pytest.fixture()
def s3():
    with mock_aws():
        client = boto3.client("s3", region_name="us-east-1")
        client.create_bucket(Bucket="test-bucket")
        init_structure(client, "test-bucket")
        yield client


@pytest.fixture()
def cfg():
    parser = configparser.ConfigParser()
    parser["arvan"] = {
        "endpoint": "https://s3.mock.local",
        "region": "ir-thr-at1",
        "bucket": "test-bucket",
        "access_key": "test",
        "secret_key": "test",
    }
    return parser


def test_init_structure_creates_folder_markers(s3):
    keys = {o["Key"] for o in s3.list_objects_v2(Bucket="test-bucket").get("Contents", [])}
    assert keys == {"posts/", "comments/"}


def test_upload_post_without_image(s3, cfg):
    post_id = upload_post(s3, bucket_name(cfg), "Ali", "Hello world!", None)

    body = s3.get_object(Bucket="test-bucket", Key=post_json_key(post_id))["Body"].read()
    post = json.loads(body.decode("utf-8"))

    assert post["post_id"] == post_id
    assert post["author"] == "Ali"
    assert post["text"] == "Hello world!"
    assert post["media_url"] is None
    assert post["media_type"] is None
    assert isinstance(post["timestamp"], int)
    # likes are never stored on the post JSON (race-safe design)
    assert "likes" not in post


def test_upload_post_with_image_uploads_media_first(s3, cfg, tmp_path):
    image = tmp_path / "photo.jpg"
    image.write_bytes(b"\xff\xd8fake-jpeg")

    post_id = upload_post(s3, bucket_name(cfg), "Sara", "Look!", str(image))

    keys = {o["Key"] for o in s3.list_objects_v2(Bucket="test-bucket").get("Contents", [])}
    assert post_json_key(post_id) in keys
    assert post_media_key(post_id, "photo.jpg") in keys
    post = json.loads(s3.get_object(Bucket="test-bucket", Key=post_json_key(post_id))["Body"].read())
    assert post["media_url"] == post_media_key(post_id, "photo.jpg")
    assert post["media_type"] == "image"
    assert post["media_name"] == "photo.jpg"
    assert post["media_mime"] == "image/jpeg"


def test_upload_post_with_any_file_type(s3, cfg, tmp_path):
    pdf = tmp_path / "report.pdf"
    pdf.write_bytes(b"%PDF-1.4 fake")

    post_id = upload_post(s3, bucket_name(cfg), "Sara", "Here's the report", str(pdf))

    keys = {o["Key"] for o in s3.list_objects_v2(Bucket="test-bucket").get("Contents", [])}
    assert post_media_key(post_id, "report.pdf") in keys
    post = json.loads(s3.get_object(Bucket="test-bucket", Key=post_json_key(post_id))["Body"].read())
    assert post["media_type"] == "file"
    assert post["media_name"] == "report.pdf"
    assert post["media_mime"] == "application/pdf"


def test_list_posts_newest_first(s3, cfg):
    upload_post(s3, bucket_name(cfg), "Ali", "first", None)
    upload_post(s3, bucket_name(cfg), "Sara", "second", None)

    posts = list_posts(s3, bucket_name(cfg))
    assert [p["author"] for p in posts] == ["Sara", "Ali"]


def test_likes_use_marker_files_not_post_json(s3, cfg):
    post_id = upload_post(s3, bucket_name(cfg), "Ali", "hi", None)

    assert toggle_like(s3, bucket_name(cfg), post_id, "Reza") is True
    assert toggle_like(s3, bucket_name(cfg), post_id, "Sara") is True

    assert post_likes(s3, bucket_name(cfg), post_id) == ["Reza", "Sara"]

    # unlike - the marker file is removed
    assert toggle_like(s3, bucket_name(cfg), post_id, "Reza") is False
    assert post_likes(s3, bucket_name(cfg), post_id) == ["Sara"]


def test_comments_roundtrip(s3, cfg):
    post_id = upload_post(s3, bucket_name(cfg), "Ali", "hi", None)

    upload_comment(s3, bucket_name(cfg), post_id, "Reza", "nice!")
    upload_comment(s3, bucket_name(cfg), post_id, "Sara", "agreed")

    comments = post_comments(s3, bucket_name(cfg), post_id)
    assert [c["author"] for c in comments] == ["Reza", "Sara"]
    assert [c["text"] for c in comments] == ["nice!", "agreed"]
    # like markers do not leak into comments
    toggle_like(s3, bucket_name(cfg), post_id, "Sara")
    assert len(post_comments(s3, bucket_name(cfg), post_id)) == 2


def test_comment_like_folder_is_per_post(s3, cfg):
    p1 = upload_post(s3, bucket_name(cfg), "Ali", "one", None)
    p2 = upload_post(s3, bucket_name(cfg), "Ali", "two", None)

    toggle_like(s3, bucket_name(cfg), p1, "Reza")
    upload_comment(s3, bucket_name(cfg), p2, "Sara", "on p2")

    assert comments_folder(p1) != comments_folder(p2)
    assert post_likes(s3, bucket_name(cfg), p2) == []
    assert post_comments(s3, bucket_name(cfg), p1) == []
    assert len(post_comments(s3, bucket_name(cfg), p2)) == 1


# ───────────────────────────────────────────────────────────────────────────── #
#  Voice notes + key sanitization (must mirror the Android side exactly)
# ───────────────────────────────────────────────────────────────────────────── #

def test_upload_post_with_voice_wav_is_audio(s3, cfg, tmp_path):
    voice = tmp_path / "voice_123.wav"
    voice.write_bytes(b"RIFFfake-wav")

    post_id = upload_post(s3, bucket_name(cfg), "Ali", "Listen!", str(voice))

    post = json.loads(
        s3.get_object(Bucket="test-bucket", Key=post_json_key(post_id))["Body"].read())
    assert post["media_type"] == "audio"
    assert post["media_mime"] == "audio/wav"
    assert post["media_url"] == post_media_key(post_id, "voice_123.wav")


def test_upload_post_with_voice_m4a_is_audio(s3, cfg, tmp_path):
    voice = tmp_path / "voice_rec.m4a"
    voice.write_bytes(b"ffffake-m4a")

    post_id = upload_post(s3, bucket_name(cfg), "Ali", "From Android!", str(voice))

    post = json.loads(
        s3.get_object(Bucket="test-bucket", Key=post_json_key(post_id))["Body"].read())
    assert post["media_type"] == "audio"
    assert post["media_mime"] == "audio/mp4"


def test_guess_mime_covers_audio_formats():
    assert guess_mime("note.m4a") == "audio/mp4"
    assert guess_mime("note.aac") == "audio/aac"
    assert guess_mime("note.ogg") == "audio/ogg"
    assert guess_mime("song.mp3") == "audio/mpeg"


def test_usernames_are_sanitized_in_keys():
    assert sanitize_user("Ali Reza") == "Ali_Reza"
    assert sanitize_user("  ع:=لی ") == "_____"
    assert sanitize_user("") == "user"
    assert sanitize_user("Reza") == "Reza"

    post_id = make_post_id("Ali Reza")
    assert post_id.endswith("_Ali_Reza")


def test_filenames_are_sanitized_ascii_only():
    # MUST match Android's Keys.sanitizeFilename byte-for-byte:
    assert sanitize_filename("عکس.jpg") == "___.jpg"
    assert sanitize_filename("my photo (1).png") == "my_photo__1_.png"
    assert sanitize_filename("report.pdf") == "report.pdf"


def test_like_keys_are_sanitized(s3, cfg):
    post_id = upload_post(s3, bucket_name(cfg), "Ali", "hi", None)
    toggle_like(s3, bucket_name(cfg), post_id, "Sara M")
    assert post_likes(s3, bucket_name(cfg), post_id) == ["Sara_M"]
