package com.arvanshare.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Key layout in the shared ArvanCloud bucket.
 * MUST stay in sync with python/arvanshare.py (same contract).
 */
object Keys {
    const val POST_PREFIX = "posts/"
    const val COMMENTS_PREFIX = "comments/"

    fun postJsonKey(postId: String) = "$POST_PREFIX${postId}_post.json"

    /** Media key keeps the original filename so any file type round-trips. */
    fun postMediaKey(postId: String, filename: String): String =
        "$POST_PREFIX${postId}_${sanitizeFilename(filename)}"

    /** Keep filenames S3-key safe (no slashes, no control chars). */
    fun sanitizeFilename(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        return base.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file" }
    }

    fun commentsFolder(postId: String) = "$COMMENTS_PREFIX$postId/"

    /** Like = an empty marker file. Creating/deleting it is an atomic like/unlike. */
    fun likeKey(postId: String, user: String) = "${commentsFolder(postId)}like_$user.txt"

    fun commentKey(postId: String, user: String, stamp: String) =
        "${commentsFolder(postId)}${stamp}_$user.json"

    fun isPostJsonKey(key: String) = key.startsWith(POST_PREFIX) && key.endsWith("_post.json")

    /** Millisecond precision keeps rapid posts by the same user unique. */
    fun makePostId(user: String): String = "${nowStamp()}_$user"

    fun nowStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
}
