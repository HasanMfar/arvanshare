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
}
