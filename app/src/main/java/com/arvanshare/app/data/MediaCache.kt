package com.arvanshare.app.data

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Small on-disk cache for S3 media, keyed by the object key. */
class MediaCache(context: Context) {
    private val dir = File(context.cacheDir, "s3media").apply { mkdirs() }

    fun fileFor(key: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    fun get(key: String): File? = fileFor(key).takeIf { it.exists() && it.length() > 0L }

    /** Trim the cache to [maxBytes], evicting the oldest files first. */
    fun trim(maxBytes: Long = DEFAULT_MAX_BYTES) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private companion object {
        const val DEFAULT_MAX_BYTES = 128L * 1024 * 1024 // 128 MB
    }
}
