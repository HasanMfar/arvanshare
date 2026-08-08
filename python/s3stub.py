#!/usr/bin/env python3
"""Minimal flat, file-backed S3-compatible stub for LOCAL testing only.

Implements just the operations ArvanShare uses (ListObjectsV2, GetObject,
PutObject, DeleteObject, HeadObject, CreateBucket) over plain HTTP so the
Android app (via http://10.0.2.2:PORT) and the Python CLI can both talk to it
without TLS/cert complications. Keys are flat (like real S3 - no folders);
each object is stored as one file. Never use this against real data.
"""

import hashlib
import os
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs, quote, unquote

ROOT = os.environ.get("S3STUB_ROOT", os.path.join(os.path.dirname(__file__), ".s3stub"))


def xml_escape(text: str) -> str:
    return (text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


class Store:
    """Flat key->file mapping per bucket. One file per object, name = sha1(key)."""

    def __init__(self, root: str):
        self.root = root

    def bucket_dir(self, bucket: str) -> str:
        return os.path.join(self.root, quote(bucket, safe=""))

    def object_path(self, bucket: str, key: str) -> str:
        digest = hashlib.sha1(key.encode("utf-8")).hexdigest()
        return os.path.join(self.bucket_dir(bucket), digest)

    def list_keys(self, bucket: str) -> list[tuple[str, int, float]]:
        """Return [(key, size, mtime)] for all objects in the bucket."""
        bdir = self.bucket_dir(bucket)
        if not os.path.isdir(bdir):
            return []
        out = []
        for name in os.listdir(bdir):
            path = os.path.join(bdir, name)
            if os.path.isfile(path):
                # metadata sidecar holds the real key
                meta_path = path + ".meta"
                try:
                    with open(meta_path, "r", encoding="utf-8") as f:
                        key = f.read()
                except OSError:
                    continue
                out.append((key, os.path.getsize(path), os.path.getmtime(path)))
        return out


def _looks_chunked(body: bytes) -> bool:
    """True if the body starts with a hex-size;chunk-signature= line."""
    line_end = body.find(b"\r\n")
    if line_end < 0:
        return False
    header = body[:line_end]
    if b";chunk-signature=" not in header:
        return False
    size_token = header.split(b";", 1)[0].strip()
    try:
        return int(size_token, 16) >= 0
    except ValueError:
        return False


def decode_aws_chunked(body: bytes) -> bytes:
    """Decode the SigV4 streaming (aws-chunked) body format.

    The AWS SDKs upload small payloads as:
        <hex-size>;chunk-signature=<sig>\\r\\n<data>\\r\\n
        ...
        0;chunk-signature=<sig>\\r\\n\\r\\n
    Real S3 decodes this server-side; a faithful stub must too.
    """
    out = bytearray()
    pos = 0
    n = len(body)
    while pos < n:
        line_end = body.find(b"\r\n", pos)
        if line_end < 0:
            # not chunked framing - return unchanged
            return body
        header = body[pos:line_end]
        pos = line_end + 2
        size_token = header.split(b";", 1)[0].strip()
        try:
            size = int(size_token, 16)
        except ValueError:
            return body  # not chunked
        if size == 0:
            break  # terminal chunk
        out += body[pos:pos + size]
        pos += size
        # skip the CRLF after chunk data
        if body[pos:pos + 2] == b"\r\n":
            pos += 2
    return bytes(out)


STORE = Store(ROOT)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send(self, code: int, body: bytes = b"", content_type: str = "application/xml"):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body and self.command != "HEAD":
            self.wfile.write(body)

    def _read_body(self) -> bytes:
        length = int(self.headers.get("Content-Length", 0) or 0)
        return self.rfile.read(length) if length else b""

    def _split(self) -> tuple[str, str] | None:
        parsed = urlparse(self.path)
        parts = [p for p in parsed.path.split("/") if p]
        if not parts:
            return None
        bucket = unquote(parts[0])
        key = "/".join(unquote(p) for p in parts[1:])
        return bucket, key

    def _list_objects_v2(self) -> bool:
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        if "list-type" not in params or params["list-type"] != ["2"]:
            return False
        parts = [p for p in parsed.path.split("/") if p]
        if not parts:
            self._send(400, b"")
            return True
        bucket = unquote(parts[0])
        if not os.path.isdir(STORE.bucket_dir(bucket)):
            self._send(404, b"<Error><Code>NoSuchBucket</Code></Error>")
            return True
        prefix = params.get("prefix", [""])[0]
        keys = [(k, s, m) for (k, s, m) in STORE.list_keys(bucket) if k.startswith(prefix)]
        keys.sort()
        contents = []
        for key, size, mtime in keys:
            contents.append(
                "<Contents>"
                f"<Key>{xml_escape(key)}</Key>"
                f"<LastModified>{time.strftime('%Y-%m-%dT%H:%M:%S.000Z', time.gmtime(mtime))}</LastModified>"
                f'<ETag>"stub"</ETag>'
                f"<Size>{size}</Size>"
                "<StorageClass>STANDARD</StorageClass>"
                "</Contents>"
            )
        body = (
            '<?xml version="1.0" encoding="UTF-8"?>'
            '<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">'
            f"<Name>{xml_escape(bucket)}</Name>"
            f"<Prefix>{xml_escape(prefix)}</Prefix>"
            f"<KeyCount>{len(keys)}</KeyCount>"
            f"<MaxKeys>{params.get('max-keys', ['1000'])[0]}</MaxKeys>"
            "<IsTruncated>false</IsTruncated>"
            + "".join(contents)
            + "</ListBucketResult>"
        ).encode()
        self._send(200, body)
        return True

    def _handle(self):
        parsed = urlparse(self.path)
        parts = [p for p in parsed.path.split("/") if p]

        # Bucket-level
        if len(parts) == 1:
            bucket = unquote(parts[0])
            if self.command == "PUT":
                os.makedirs(STORE.bucket_dir(bucket), exist_ok=True)
                self._send(200)
                return
            if self.command == "GET" and self._list_objects_v2():
                return
            self._send(200, b"")
            return

        split = self._split()
        if split is None:
            self._send(400, b"")
            return
        bucket, key = split
        path = STORE.object_path(bucket, key)

        if self.command in ("PUT", "POST"):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            raw = self._read_body()
            # The AWS SDK may send SigV4 streaming (aws-chunked); decode like real S3.
            # Header check alone is unreliable (the SDK doesn't always send
            # Content-Encoding: aws-chunked), so auto-detect the framing.
            if self.headers.get("Content-Encoding", "") == "aws-chunked" or _looks_chunked(raw):
                raw = decode_aws_chunked(raw)
            with open(path, "wb") as f:
                f.write(raw)
            with open(path + ".meta", "w", encoding="utf-8") as f:
                f.write(key)
            self._send(200, b"")
            return
        if self.command in ("GET", "HEAD"):
            if os.path.isfile(path):
                with open(path, "rb") as f:
                    body = f.read()
                self._send(200, body, content_type="application/octet-stream")
            else:
                self._send(404, b"<Error><Code>NoSuchKey</Code></Error>")
            return
        if self.command == "DELETE":
            if os.path.isfile(path):
                os.remove(path)
                if os.path.exists(path + ".meta"):
                    os.remove(path + ".meta")
            self._send(204)
            return
        self._send(405, b"")

    do_GET = do_PUT = do_POST = do_DELETE = do_HEAD = _handle

    def log_message(self, fmt, *args):
        sys.stderr.write("[s3stub] %s - %s\n" % (self.address_string(), fmt % args))


def main():
    port = int(os.environ.get("S3STUB_PORT", "5000"))
    os.makedirs(ROOT, exist_ok=True)
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"s3stub listening on http://0.0.0.0:{port}  (root={ROOT})", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
