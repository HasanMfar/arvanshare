package com.arvanshare.app.data

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Key layout in the shared ArvanCloud bucket.
 * Supports root feed (backward compatible) and private rooms with optional passwords.
 * MUST stay in sync with python/arvanshare.py (same contract).
 */
object Keys {
    const val POST_PREFIX = "posts/"
    const val COMMENTS_PREFIX = "comments/"
    const val ROOMS_PREFIX = "rooms/"

    /** Keep filenames, usernames, and room names S3-key safe (ASCII only). */
    fun sanitizeSegment(value: String, fallback: String): String {
        val base = value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return base.ifBlank { fallback }
    }

    fun sanitizeFilename(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        return sanitizeSegment(base, "file")
    }

    fun sanitizeName(name: String): String = sanitizeSegment(name, "user")

    fun sanitizeRoom(room: String): String = sanitizeSegment(room, "general")

    fun isDefaultRoom(room: String?): Boolean =
        room.isNullOrBlank() || room.equals("default", ignoreCase = true) || room.equals("main", ignoreCase = true)

    fun postPrefix(room: String = ""): String =
        if (isDefaultRoom(room)) POST_PREFIX else "$ROOMS_PREFIX${sanitizeRoom(room)}/$POST_PREFIX"

    fun commentsPrefix(room: String = ""): String =
        if (isDefaultRoom(room)) COMMENTS_PREFIX else "$ROOMS_PREFIX${sanitizeRoom(room)}/$COMMENTS_PREFIX"

    fun roomInfoKey(room: String): String =
        "$ROOMS_PREFIX${sanitizeRoom(room)}/room.json"

    fun postJsonKey(postId: String, room: String = ""): String =
        "${postPrefix(room)}${postId}_post.json"

    /** Media key keeps the original filename so any file type round-trips. */
    fun postMediaKey(postId: String, filename: String, room: String = ""): String =
        "${postPrefix(room)}${postId}_${sanitizeFilename(filename)}"

    fun commentsFolder(postId: String, room: String = ""): String =
        "${commentsPrefix(room)}$postId/"

    /** Like = an empty marker file. Creating/deleting it is an atomic like/unlike. */
    fun likeKey(postId: String, user: String, room: String = ""): String =
        "${commentsFolder(postId, room)}like_${sanitizeName(user)}.txt"

    fun commentKey(postId: String, user: String, stamp: String, room: String = ""): String =
        "${commentsFolder(postId, room)}${stamp}_${sanitizeName(user)}.json"

    fun isPostJsonKey(key: String, room: String = ""): Boolean {
        val prefix = postPrefix(room)
        return key.startsWith(prefix) && key.endsWith("_post.json")
    }

    fun postIdFromJsonKey(key: String, room: String = ""): String =
        key.removePrefix(postPrefix(room)).removeSuffix("_post.json")

    /** Millisecond precision keeps rapid posts by the same user unique. */
    fun makePostId(user: String): String = "${nowStamp()}_${sanitizeName(user)}"

    fun nowStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())

    /** SHA-256 hex string for room password protection. */
    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
