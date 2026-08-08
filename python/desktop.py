#!/usr/bin/env python3
"""ArvanShare desktop client — modernised Tkinter UI.

Dark-mode by default, card-style feed, author avatar badges, animated
progress indicator, and a polished settings / compose dialog.

Run:  python desktop.py          (from .venv or system Python with boto3)
      dist/ArvanShare.exe        (portable build — see build_exe.bat)
"""

import configparser
import os
import queue
import tempfile
import threading
import tkinter as tk
from datetime import datetime
from tkinter import filedialog, messagebox, ttk

from arvanshare import (
    build_client,
    bucket_name,
    init_structure,
    list_posts,
    post_comments,
    post_likes,
    toggle_like,
    upload_comment,
    upload_post,
)

HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.path.join(HERE, "config.ini")

# ───────────────────────────────────────────────────────────────────────────── #
#  Theme tokens — tweak these to restyle the whole app
# ───────────────────────────────────────────────────────────────────────────── #

THEME = {
    # surfaces
    "bg":           "#12131A",   # window background (very dark blue-black)
    "surface":      "#1E2030",   # card / panel surface
    "surface2":     "#252840",   # slightly lighter panel (toolbar, inputs)
    "border":       "#333560",   # subtle border / separator
    # text
    "fg":           "#E8EAF6",   # primary text
    "fg2":          "#9FA8DA",   # secondary / muted text
    "fg3":          "#5C6497",   # very muted / timestamps
    # brand
    "primary":      "#5C6BC0",   # indigo
    "primary_dark": "#3949AB",
    "accent":       "#4DD0E1",   # teal
    "error":        "#EF5350",
    "success":      "#66BB6A",
    # avatar palette (cycles by author hash)
    "avatars":      ["#5C6BC0", "#00BCD4", "#66BB6A", "#FF7043",
                     "#AB47BC", "#26A69A", "#EC407A", "#FFA726"],
    # fonts (tk font tuples)
    "font_heading": ("Segoe UI", 14, "bold"),
    "font_body":    ("Segoe UI", 10),
    "font_small":   ("Segoe UI", 9),
    "font_tiny":    ("Segoe UI", 8),
    "font_mono":    ("Consolas", 9),
}

SPINNER_FRAMES = ("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")


# ───────────────────────────────────────────────────────────────────────────── #
#  Helpers
# ───────────────────────────────────────────────────────────────────────────── #

def apply_dark_theme(root: tk.Tk) -> None:
    """Configure ttk style and root background for the dark theme."""
    style = ttk.Style(root)
    try:
        style.theme_use("clam")
    except Exception:
        pass

    bg   = THEME["bg"]
    sur  = THEME["surface"]
    sur2 = THEME["surface2"]
    fg   = THEME["fg"]
    fg2  = THEME["fg2"]
    pri  = THEME["primary"]
    bdr  = THEME["border"]

    style.configure(".",
        background=bg, foreground=fg,
        fieldbackground=sur2, troughcolor=sur2,
        selectbackground=pri, selectforeground="#ffffff",
        font=THEME["font_body"],
    )
    style.configure("TFrame",    background=bg)
    style.configure("TLabel",    background=bg, foreground=fg)
    style.configure("TButton",
        background=sur2, foreground=fg,
        bordercolor=bdr, focuscolor=pri,
        padding=(10, 5),
    )
    style.map("TButton",
        background=[("active", pri), ("pressed", THEME["primary_dark"])],
        foreground=[("active", "#ffffff")],
    )
    style.configure("Accent.TButton",
        background=pri, foreground="#ffffff",
        bordercolor=pri, padding=(12, 6),
    )
    style.map("Accent.TButton",
        background=[("active", THEME["primary_dark"])],
    )
    style.configure("TEntry",
        fieldbackground=sur2, foreground=fg, insertcolor=fg,
        bordercolor=bdr, lightcolor=bdr, darkcolor=bdr,
    )
    style.configure("TScrollbar",
        background=sur, troughcolor=bg,
        arrowcolor=fg2, bordercolor=bdr,
    )
    style.configure("Treeview",
        background=bg, foreground=fg,
        fieldbackground=bg,
        rowheight=54,
        bordercolor=bdr,
    )
    style.configure("Treeview.Heading",
        background=sur2, foreground=fg2,
        bordercolor=bdr, font=THEME["font_small"],
    )
    style.map("Treeview",
        background=[("selected", sur2)],
        foreground=[("selected", fg)],
    )
    style.configure("Card.TFrame", background=sur, relief="flat")
    style.configure("Toolbar.TFrame", background=sur2)

    root.configure(bg=bg)


def fmt_time(ts: int) -> str:
    return datetime.fromtimestamp(ts).strftime("%b %d · %H:%M")


def avatar_color(name: str) -> str:
    colors = THEME["avatars"]
    return colors[abs(hash(name)) % len(colors)]


def avatar_initial(name: str) -> str:
    return (name or "?")[0].upper()


def run_async(widget, fn, on_done) -> None:
    """Run fn() in a worker thread; call on_done(kind, payload) on the UI thread."""
    q: queue.Queue = queue.Queue()

    def worker():
        try:
            q.put(("ok", fn()))
        except Exception as exc:  # noqa: BLE001
            q.put(("error", exc))

    def poll():
        try:
            kind, payload = q.get_nowait()
        except queue.Empty:
            widget.after(100, poll)
            return
        on_done(kind, payload)

    threading.Thread(target=worker, daemon=True).start()
    widget.after(100, poll)


def load_config_dict() -> dict:
    cfg = configparser.ConfigParser()
    if os.path.exists(CONFIG_PATH):
        cfg.read(CONFIG_PATH)
        return dict(cfg["arvan"]) if "arvan" in cfg else {}
    return {}


def save_config_dict(values: dict) -> None:
    cfg = configparser.ConfigParser()
    cfg["arvan"] = {
        "endpoint":   values.get("endpoint", ""),
        "region":     values.get("region", "ir-thr-at1"),
        "bucket":     values.get("bucket", ""),
        "access_key": values.get("access_key", ""),
        "secret_key": values.get("secret_key", ""),
    }
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        cfg.write(f)


def client_from(values: dict):
    cfg = configparser.ConfigParser()
    cfg["arvan"] = values
    return build_client(cfg), values.get("bucket", ""), values.get("name", "")


# ───────────────────────────────────────────────────────────────────────────── #
#  Avatar canvas widget
# ───────────────────────────────────────────────────────────────────────────── #

class AvatarCanvas(tk.Canvas):
    """Small circular avatar showing the author's initial letter."""

    def __init__(self, parent, name: str, size: int = 36, **kwargs):
        super().__init__(parent, width=size, height=size,
                         highlightthickness=0, bd=0,
                         bg=THEME["surface"], **kwargs)
        color = avatar_color(name)
        r = size // 2
        self.create_oval(1, 1, size - 1, size - 1, fill=color, outline="")
        self.create_text(r, r, text=avatar_initial(name),
                         fill="#ffffff", font=("Segoe UI", max(r - 4, 8), "bold"))


# ───────────────────────────────────────────────────────────────────────────── #
#  Spinner label
# ───────────────────────────────────────────────────────────────────────────── #

class Spinner(tk.Label):
    def __init__(self, parent, **kwargs):
        super().__init__(parent, text=SPINNER_FRAMES[0],
                         fg=THEME["primary"], bg=THEME["bg"],
                         font=THEME["font_body"], **kwargs)
        self._frame = 0
        self._job = None

    def start(self):
        self._tick()

    def stop(self):
        if self._job:
            self.after_cancel(self._job)
            self._job = None
        self.config(text="")

    def _tick(self):
        self._frame = (self._frame + 1) % len(SPINNER_FRAMES)
        self.config(text=SPINNER_FRAMES[self._frame])
        self._job = self.after(80, self._tick)


# ───────────────────────────────────────────────────────────────────────────── #
#  Settings dialog
# ───────────────────────────────────────────────────────────────────────────── #

class SetupDialog(tk.Toplevel):
    """First-run / settings dialog: display name, endpoint, bucket, keys."""

    FIELDS = [
        ("name",       "Display name",   False, "e.g. Ali"),
        ("endpoint",   "S3 endpoint",    False, "https://s3.ir-thr-at1.arvanstorage.ir"),
        ("bucket",     "Bucket name",    False, "my-family-share"),
        ("access_key", "Access key",     False, ""),
        ("secret_key", "Secret key",     True,  ""),
    ]

    def __init__(self, root, values: dict, on_saved, on_cancel):
        super().__init__(root)
        self.title("ArvanShare — Connection settings")
        self.resizable(False, False)
        self.configure(bg=THEME["bg"])
        self.on_saved  = on_saved
        self.on_cancel = on_cancel
        self.transient(root)
        self.grab_set()

        self.vars = {f[0]: tk.StringVar(value=values.get(f[0], "")) for f in self.FIELDS}

        # ── Header ──────────────────────────────────────────────────────────
        hdr = tk.Frame(self, bg=THEME["surface2"], padx=20, pady=12)
        hdr.pack(fill="x")
        tk.Label(hdr, text="⚙  Connection Settings",
                 bg=THEME["surface2"], fg=THEME["fg"],
                 font=THEME["font_heading"]).pack(anchor="w")
        tk.Label(hdr, text="Saved only on this device — never sent anywhere else.",
                 bg=THEME["surface2"], fg=THEME["fg2"],
                 font=THEME["font_small"]).pack(anchor="w", pady=(2, 0))

        # ── Fields ──────────────────────────────────────────────────────────
        body = tk.Frame(self, bg=THEME["bg"], padx=20, pady=16)
        body.pack(fill="both", expand=True)

        self.entries = {}
        for row, (key, label, secret, placeholder) in enumerate(self.FIELDS):
            tk.Label(body, text=label, bg=THEME["bg"], fg=THEME["fg2"],
                     font=THEME["font_small"]).grid(row=row * 2, column=0, sticky="w", pady=(8, 0))
            entry = tk.Entry(body, textvariable=self.vars[key], width=46,
                             bg=THEME["surface2"], fg=THEME["fg"],
                             insertbackground=THEME["fg"], relief="flat",
                             font=THEME["font_body"],
                             show="•" if secret else "")
            entry.grid(row=row * 2 + 1, column=0, sticky="ew", pady=(2, 0), ipady=6, padx=(0, 4))
            if placeholder and not self.vars[key].get():
                self._add_placeholder(entry, placeholder)
            self.entries[key] = entry

        body.columnconfigure(0, weight=1)

        # ── Status ──────────────────────────────────────────────────────────
        self._status_var = tk.StringVar()
        self._status_lbl = tk.Label(body, textvariable=self._status_var,
                                    bg=THEME["bg"], fg=THEME["fg2"],
                                    font=THEME["font_small"], wraplength=360, justify="left")
        self._status_lbl.grid(row=len(self.FIELDS) * 2, column=0, sticky="w", pady=(12, 0))

        # ── Buttons ─────────────────────────────────────────────────────────
        btns = tk.Frame(self, bg=THEME["bg"], padx=20, pady=12)
        btns.pack(fill="x")
        self._test_btn = tk.Button(btns, text="Test connection",
                                   command=self.test, **self._btn_style())
        self._test_btn.pack(side="left", padx=(0, 6))
        tk.Button(btns, text="Save", command=self.save,
                  **self._btn_style(accent=True)).pack(side="left", padx=(0, 6))
        tk.Button(btns, text="Cancel", command=self.cancel,
                  **self._btn_style()).pack(side="left")

        self.bind("<Return>", lambda _e: self.save())

    def _btn_style(self, accent=False) -> dict:
        return dict(
            bg=THEME["primary"] if accent else THEME["surface2"],
            fg="#ffffff" if accent else THEME["fg"],
            activebackground=THEME["primary_dark"] if accent else THEME["primary"],
            activeforeground="#ffffff",
            relief="flat", padx=14, pady=6,
            font=THEME["font_body"], cursor="hand2",
        )

    def _add_placeholder(self, entry: tk.Entry, text: str):
        entry.insert(0, text)
        entry.config(fg=THEME["fg3"])

        def on_focus_in(_e):
            if entry.get() == text:
                entry.delete(0, "end")
                entry.config(fg=THEME["fg"])

        def on_focus_out(_e):
            if not entry.get():
                entry.insert(0, text)
                entry.config(fg=THEME["fg3"])

        entry.bind("<FocusIn>",  on_focus_in)
        entry.bind("<FocusOut>", on_focus_out)

    def _values(self) -> dict:
        return {k: v.get().strip() for k, v in self.vars.items()}

    def _valid(self, values: dict) -> bool:
        missing = [k for k in ("name", "endpoint", "bucket", "access_key", "secret_key")
                   if not values.get(k)]
        if missing:
            messagebox.showwarning("Missing fields", "Please fill in: " + ", ".join(missing),
                                   parent=self)
            return False
        return True

    def test(self):
        values = self._values()
        if not self._valid(values):
            return
        self._test_btn.config(state="disabled")
        self._status_var.set("Testing connection…")
        self._status_lbl.config(fg=THEME["fg2"])
        cfg = configparser.ConfigParser()
        cfg["arvan"] = values
        client = build_client(cfg)

        def work():
            client.list_objects_v2(Bucket=bucket_name(cfg), Prefix="posts/", MaxKeys=1)

        def done(kind, payload):
            self._test_btn.config(state="normal")
            if kind == "ok":
                self._status_var.set("✓  Connection successful!")
                self._status_lbl.config(fg=THEME["success"])
            else:
                self._status_var.set(f"✗  {payload}")
                self._status_lbl.config(fg=THEME["error"])

        run_async(self, work, done)

    def save(self):
        values = self._values()
        if not self._valid(values):
            return
        save_config_dict(values)
        self.on_saved(values)
        self.destroy()

    def cancel(self):
        self.on_cancel()
        self.destroy()


# ───────────────────────────────────────────────────────────────────────────── #
#  Compose dialog
# ───────────────────────────────────────────────────────────────────────────── #

class ComposeDialog(tk.Toplevel):
    def __init__(self, root, values: dict, on_posted):
        super().__init__(root)
        self.title("New post")
        self.geometry("520x380")
        self.configure(bg=THEME["bg"])
        self.resizable(False, False)
        self.transient(root)
        self.grab_set()
        self.values   = values
        self.on_posted = on_posted
        self._file_path = None

        # ── Header ──────────────────────────────────────────────────────────
        hdr = tk.Frame(self, bg=THEME["surface2"], padx=16, pady=10)
        hdr.pack(fill="x")
        tk.Label(hdr, text=f"✏  Posting as  {values.get('name', '')}",
                 bg=THEME["surface2"], fg=THEME["primary"],
                 font=THEME["font_heading"]).pack(anchor="w")

        # ── Text area ───────────────────────────────────────────────────────
        body = tk.Frame(self, bg=THEME["bg"], padx=16, pady=12)
        body.pack(fill="both", expand=True)

        self._text = tk.Text(body, height=7,
                             bg=THEME["surface2"], fg=THEME["fg"],
                             insertbackground=THEME["fg"], relief="flat",
                             font=THEME["font_body"], padx=10, pady=8,
                             wrap="word")
        self._text.pack(fill="both", expand=True)

        # placeholder
        self._placeholder = "What's on your mind?"
        self._text.insert("1.0", self._placeholder)
        self._text.config(fg=THEME["fg3"])
        self._text.bind("<FocusIn>",  self._on_text_focus_in)
        self._text.bind("<FocusOut>", self._on_text_focus_out)

        # ── Attachment label ─────────────────────────────────────────────
        self._file_lbl = tk.Label(body, text="📎  No file attached",
                                  bg=THEME["bg"], fg=THEME["fg3"],
                                  font=THEME["font_small"], anchor="w")
        self._file_lbl.pack(fill="x", pady=(6, 0))

        # ── Buttons ─────────────────────────────────────────────────────────
        btns = tk.Frame(self, bg=THEME["bg"], padx=16, pady=10)
        btns.pack(fill="x")
        tk.Button(btns, text="Attach file…", command=self._pick_file,
                  **self._btn_style()).pack(side="left", padx=(0, 6))
        self._pub_btn = tk.Button(btns, text="Publish →", command=self._publish,
                                  **self._btn_style(accent=True))
        self._pub_btn.pack(side="left")
        tk.Button(btns, text="Cancel", command=self.destroy,
                  **self._btn_style()).pack(side="right")

    def _btn_style(self, accent=False) -> dict:
        return dict(
            bg=THEME["primary"] if accent else THEME["surface2"],
            fg="#ffffff" if accent else THEME["fg"],
            activebackground=THEME["primary_dark"] if accent else THEME["primary"],
            activeforeground="#ffffff",
            relief="flat", padx=14, pady=6,
            font=THEME["font_body"], cursor="hand2",
        )

    def _on_text_focus_in(self, _e):
        if self._text.get("1.0", "end-1c") == self._placeholder:
            self._text.delete("1.0", "end")
            self._text.config(fg=THEME["fg"])

    def _on_text_focus_out(self, _e):
        if not self._text.get("1.0", "end-1c").strip():
            self._text.insert("1.0", self._placeholder)
            self._text.config(fg=THEME["fg3"])

    def _pick_file(self):
        path = filedialog.askopenfilename(
            title="Attach a file (any type)", filetypes=[("All files", "*.*")], parent=self)
        if path:
            self._file_path = path
            name = os.path.basename(path)
            self._file_lbl.config(text=f"📎  {name}", fg=THEME["fg"])

    def _publish(self):
        raw = self._text.get("1.0", "end-1c").strip()
        text = "" if raw == self._placeholder else raw
        if not text and not self._file_path:
            messagebox.showwarning("Empty post", "Write something or attach a file.", parent=self)
            return
        self._pub_btn.config(state="disabled", text="Publishing…")

        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            return upload_post(build_client(cfg), self.values["bucket"],
                               self.values["name"], text, self._file_path)

        def done(kind, payload):
            if kind == "ok":
                self.on_posted(payload)
                self.destroy()
            else:
                self._pub_btn.config(state="normal", text="Publish →")
                messagebox.showerror("Publish failed", str(payload), parent=self)

        run_async(self, work, done)


# ───────────────────────────────────────────────────────────────────────────── #
#  Post detail dialog
# ───────────────────────────────────────────────────────────────────────────── #

class DetailDialog(tk.Toplevel):
    def __init__(self, root, values: dict, post: dict, on_changed):
        super().__init__(root)
        author = post.get("author", "")
        self.title(f"Post by {author}")
        self.geometry("500x560")
        self.configure(bg=THEME["bg"])
        self.resizable(True, True)
        self.transient(root)
        self.grab_set()
        self.values     = values
        self.post       = post
        self.on_changed = on_changed
        self._me        = values.get("name", "")

        self._build_ui(author)
        self.load()

    def _build_ui(self, author: str):
        # ── Post header ──────────────────────────────────────────────────────
        hdr = tk.Frame(self, bg=THEME["surface2"], padx=16, pady=12)
        hdr.pack(fill="x")

        AvatarCanvas(hdr, author, size=44).pack(side="left", padx=(0, 10))

        meta = tk.Frame(hdr, bg=THEME["surface2"])
        meta.pack(side="left", fill="both")
        tk.Label(meta, text=author, bg=THEME["surface2"], fg=THEME["fg"],
                 font=THEME["font_heading"]).pack(anchor="w")
        ts = self.post.get("timestamp", 0)
        tk.Label(meta, text=fmt_time(ts), bg=THEME["surface2"], fg=THEME["fg3"],
                 font=THEME["font_small"]).pack(anchor="w")

        # Refresh button
        tk.Button(hdr, text="⟳", command=self.load,
                  bg=THEME["surface2"], fg=THEME["fg2"],
                  relief="flat", font=("Segoe UI", 12), cursor="hand2",
                  activebackground=THEME["bg"]).pack(side="right", padx=4)

        # ── Post text ────────────────────────────────────────────────────────
        body_frame = tk.Frame(self, bg=THEME["bg"], padx=16, pady=10)
        body_frame.pack(fill="x")
        text = self.post.get("text", "")
        if text:
            tk.Label(body_frame, text=text, bg=THEME["bg"], fg=THEME["fg"],
                     font=THEME["font_body"], wraplength=460, justify="left",
                     anchor="w").pack(anchor="w")

        # Media button
        if self.post.get("media_url"):
            media_name = self.post.get("media_name") or "attachment"
            media_type = self.post.get("media_type", "file")
            icon = "🖼" if media_type == "image" else "📎"
            tk.Button(body_frame, text=f"{icon}  Open {media_name}",
                      command=self._open_media,
                      bg=THEME["surface2"], fg=THEME["primary"],
                      activebackground=THEME["bg"], relief="flat",
                      font=THEME["font_small"], cursor="hand2",
                      padx=10, pady=4).pack(anchor="w", pady=(6, 0))

        # ── Divider ──────────────────────────────────────────────────────────
        tk.Frame(self, bg=THEME["border"], height=1).pack(fill="x", padx=16)

        # ── Likes row ────────────────────────────────────────────────────────
        likes_row = tk.Frame(self, bg=THEME["bg"], padx=16, pady=6)
        likes_row.pack(fill="x")
        self._like_btn = tk.Button(likes_row, text="♡  Like",
                                   command=self._toggle_like,
                                   bg=THEME["surface2"], fg=THEME["fg"],
                                   activebackground=THEME["error"],
                                   activeforeground="#ffffff",
                                   relief="flat", font=THEME["font_small"],
                                   cursor="hand2", padx=10, pady=4)
        self._like_btn.pack(side="left", padx=(0, 10))
        self._likes_lbl = tk.Label(likes_row, text="…", bg=THEME["bg"],
                                   fg=THEME["fg2"], font=THEME["font_small"])
        self._likes_lbl.pack(side="left")

        # ── Comments ─────────────────────────────────────────────────────────
        tk.Label(self, text="💬  Comments", bg=THEME["bg"], fg=THEME["fg2"],
                 font=THEME["font_small"], padx=16).pack(anchor="w", pady=(4, 0))

        comments_frame = tk.Frame(self, bg=THEME["bg"], padx=16)
        comments_frame.pack(fill="both", expand=True, pady=(4, 0))

        self._comments_text = tk.Text(
            comments_frame, state="disabled",
            bg=THEME["surface"], fg=THEME["fg"],
            relief="flat", font=THEME["font_body"],
            padx=10, pady=8, wrap="word",
            cursor="arrow",
        )
        sb = ttk.Scrollbar(comments_frame, orient="vertical",
                           command=self._comments_text.yview)
        self._comments_text.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        self._comments_text.pack(side="left", fill="both", expand=True)

        # ── Comment input ─────────────────────────────────────────────────────
        input_row = tk.Frame(self, bg=THEME["bg"], padx=16, pady=10)
        input_row.pack(fill="x")
        self._comment_entry = tk.Entry(input_row,
                                       bg=THEME["surface2"], fg=THEME["fg"],
                                       insertbackground=THEME["fg"], relief="flat",
                                       font=THEME["font_body"])
        self._comment_entry.pack(side="left", fill="x", expand=True, ipady=6, padx=(0, 8))
        self._comment_entry.bind("<Return>", lambda _e: self._send_comment())
        tk.Button(input_row, text="Send →", command=self._send_comment,
                  bg=THEME["primary"], fg="#ffffff",
                  activebackground=THEME["primary_dark"], activeforeground="#ffffff",
                  relief="flat", font=THEME["font_small"], cursor="hand2",
                  padx=12, pady=5).pack(side="left")

    # ── Actions ────────────────────────────────────────────────────────────────

    def load(self):
        post_id = self.post["post_id"]

        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            client = build_client(cfg)
            likes    = post_likes(client, self.values["bucket"], post_id)
            comments = post_comments(client, self.values["bucket"], post_id)
            return likes, comments

        def done(kind, payload):
            if kind != "ok":
                messagebox.showerror("Load failed", str(payload), parent=self)
                return
            likes, comments = payload
            liked = self._me in likes

            # Update like button
            self._like_btn.config(
                text=("❤  Unlike" if liked else "♡  Like"),
                fg=THEME["error"] if liked else THEME["fg"],
            )
            # Update likes label
            if likes:
                self._likes_lbl.config(
                    text=f"{len(likes)} like{'s' if len(likes) != 1 else ''}: {', '.join(likes)}")
            else:
                self._likes_lbl.config(text="No likes yet")

            # Update comments
            self._comments_text.config(state="normal")
            self._comments_text.delete("1.0", "end")
            if not comments:
                self._comments_text.insert("end", "  No comments yet. Be the first!\n",)
            for c in comments:
                author = c.get("author", "")
                ts     = fmt_time(c.get("timestamp", 0))
                text   = c.get("text", "")
                self._comments_text.insert(
                    "end",
                    f"  {author}  ·  {ts}\n"
                    f"  {text}\n"
                    f"\n",
                )
            self._comments_text.config(state="disabled")

        run_async(self, work, done)

    def _toggle_like(self):
        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            return toggle_like(build_client(cfg), self.values["bucket"],
                               self.post["post_id"], self._me)

        def done(kind, payload):
            if kind == "ok":
                self.on_changed()
                self.load()
            else:
                messagebox.showerror("Like failed", str(payload), parent=self)

        run_async(self, work, done)

    def _send_comment(self):
        text = self._comment_entry.get().strip()
        if not text:
            return
        self._comment_entry.delete(0, "end")

        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            upload_comment(build_client(cfg), self.values["bucket"],
                           self.post["post_id"], self._me, text)

        def done(kind, payload):
            if kind == "ok":
                self.on_changed()
                self.load()
            else:
                messagebox.showerror("Comment failed", str(payload), parent=self)

        run_async(self, work, done)

    def _open_media(self):
        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            client = build_client(cfg)
            obj = client.get_object(Bucket=self.values["bucket"], Key=self.post["media_url"])
            tmp = os.path.join(tempfile.gettempdir(),
                               os.path.basename(self.post["media_url"]))
            with open(tmp, "wb") as f:
                f.write(obj["Body"].read())
            return tmp

        def done(kind, payload):
            if kind == "ok":
                os.startfile(payload)
            else:
                messagebox.showerror("Download failed", str(payload), parent=self)

        run_async(self, work, done)


# ───────────────────────────────────────────────────────────────────────────── #
#  Main application window
# ───────────────────────────────────────────────────────────────────────────── #

class ArvanDesktopApp:
    def __init__(self, root: tk.Tk):
        self.root    = root
        self.values  = load_config_dict()
        self.posts: list[dict] = []

        root.title("ArvanShare")
        root.geometry("720x580")
        root.configure(bg=THEME["bg"])
        root.minsize(540, 420)

        apply_dark_theme(root)
        self._build_ui()

        if self._configured():
            self.refresh()
        else:
            self.open_setup()

    # ── UI ─────────────────────────────────────────────────────────────────────

    def _build_ui(self):
        # ── Toolbar ──────────────────────────────────────────────────────────
        toolbar = tk.Frame(self.root, bg=THEME["surface2"], padx=10, pady=8)
        toolbar.pack(fill="x")

        # Brand name
        tk.Label(toolbar, text="ArvanShare",
                 bg=THEME["surface2"], fg=THEME["primary"],
                 font=("Segoe UI", 13, "bold")).pack(side="left", padx=(4, 20))

        btn_kw = dict(
            bg=THEME["surface2"], fg=THEME["fg"],
            activebackground=THEME["primary"], activeforeground="#ffffff",
            relief="flat", padx=10, pady=4, font=THEME["font_body"], cursor="hand2",
        )
        tk.Button(toolbar, text="✏  New post",
                  command=self.open_compose, **btn_kw).pack(side="left", padx=3)
        tk.Button(toolbar, text="⟳  Refresh",
                  command=self.refresh,      **btn_kw).pack(side="left", padx=3)
        tk.Button(toolbar, text="⚙  Settings",
                  command=self.open_setup,   **btn_kw).pack(side="left", padx=3)

        self._user_lbl = tk.Label(toolbar, text="", bg=THEME["surface2"],
                                  fg=THEME["fg2"], font=THEME["font_small"])
        self._user_lbl.pack(side="right", padx=8)
        if self.values.get("name"):
            self._user_lbl.config(text=f"👤  {self.values['name']}")

        # ── Feed (Treeview with per-row rendering) ───────────────────────────
        feed_frame = tk.Frame(self.root, bg=THEME["bg"])
        feed_frame.pack(fill="both", expand=True, padx=0, pady=0)

        # Columns: hidden post_id, author, when, text_preview
        cols = ("author", "when", "preview")
        self._tree = ttk.Treeview(feed_frame, columns=cols, show="headings",
                                  selectmode="browse")
        self._tree.heading("author",  text="Author")
        self._tree.heading("when",    text="When")
        self._tree.heading("preview", text="Post")
        self._tree.column("author",  width=130, minwidth=80,  anchor="w", stretch=False)
        self._tree.column("when",    width=130, minwidth=90,  anchor="w", stretch=False)
        self._tree.column("preview", width=420, minwidth=200, anchor="w")

        vsb = ttk.Scrollbar(feed_frame, orient="vertical", command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side="right", fill="y")
        self._tree.pack(side="left", fill="both", expand=True)

        self._tree.bind("<Double-1>",  self.open_detail)
        self._tree.bind("<Return>",    self.open_detail)

        # ── Status bar ───────────────────────────────────────────────────────
        status_bar = tk.Frame(self.root, bg=THEME["surface2"], pady=4, padx=12)
        status_bar.pack(fill="x", side="bottom")

        self._spinner = Spinner(status_bar)
        self._spinner.pack(side="left", padx=(0, 6))

        self._status_lbl = tk.Label(status_bar, text="Ready",
                                    bg=THEME["surface2"], fg=THEME["fg2"],
                                    font=THEME["font_small"], anchor="w")
        self._status_lbl.pack(side="left", fill="x", expand=True)

        tk.Label(status_bar, text="v1.0.0-beta.1",
                 bg=THEME["surface2"], fg=THEME["fg3"],
                 font=THEME["font_tiny"]).pack(side="right")

    def _status(self, msg: str, color: str | None = None):
        self._status_lbl.config(text=msg, fg=color or THEME["fg2"])

    def _configured(self) -> bool:
        return all(self.values.get(k)
                   for k in ("name", "endpoint", "bucket", "access_key", "secret_key"))

    # ── Actions ────────────────────────────────────────────────────────────────

    def open_setup(self):
        SetupDialog(self.root, self.values,
                    on_saved=self._on_saved, on_cancel=lambda: None)

    def _on_saved(self, values: dict):
        self.values = values
        self._user_lbl.config(text=f"👤  {values.get('name', '')}")
        self._status("Settings saved.", THEME["success"])
        self.refresh()

    def open_compose(self):
        if not self._configured():
            messagebox.showwarning("Not configured",
                                   "Open Settings and enter your details first.")
            return
        ComposeDialog(self.root, self.values,
                      on_posted=lambda _pid: self.refresh())

    def open_detail(self, _event=None):
        sel = self._tree.selection()
        if not sel:
            return
        post_id = self._tree.item(sel[0])["tags"][0] if self._tree.item(sel[0])["tags"] else None
        if not post_id:
            return
        post = next((p for p in self.posts if p.get("post_id") == post_id), None)
        if post:
            DetailDialog(self.root, self.values, post, on_changed=self.refresh)

    def refresh(self):
        if not self._configured():
            return
        self._status("Loading feed…")
        self._spinner.start()

        def work():
            cfg = configparser.ConfigParser()
            cfg["arvan"] = self.values
            return list_posts(build_client(cfg), self.values["bucket"])

        def done(kind, payload):
            self._spinner.stop()
            if kind == "ok":
                self.posts = payload
                self._populate_feed(payload)
            else:
                self._status(f"Load failed: {payload}", THEME["error"])
                messagebox.showerror("Feed load failed", str(payload))

        run_async(self.root, work, done)

    def _populate_feed(self, posts: list[dict]):
        self._tree.delete(*self._tree.get_children())
        for p in posts:
            author  = p.get("author", "")
            ts      = fmt_time(p.get("timestamp", 0))
            raw     = (p.get("text") or "").replace("\n", " ")
            preview = (raw[:72] + "…" if len(raw) > 72 else raw)
            if p.get("media_url"):
                icon = "🖼" if p.get("media_type") == "image" else "📎"
                preview = f"{icon}  {preview}" if preview else icon
            iid = self._tree.insert("", "end",
                                    values=(author, ts, preview),
                                    tags=(p.get("post_id", ""),))
        count = len(posts)
        self._status(f"{count} post{'s' if count != 1 else ''} loaded.")


# ───────────────────────────────────────────────────────────────────────────── #
#  Entry point
# ───────────────────────────────────────────────────────────────────────────── #

def main():
    root = tk.Tk()
    root.withdraw()           # hide while setting up theme/dpi
    try:
        root.tk.call("tk", "scaling", 1.5)  # crisp on HiDPI displays
    except Exception:
        pass
    apply_dark_theme(root)
    root.deiconify()
    ArvanDesktopApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
