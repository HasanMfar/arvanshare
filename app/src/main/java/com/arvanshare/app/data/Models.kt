package com.arvanshare.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    @SerialName("post_id") val postId: String,
    val author: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val text: String = "",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_name") val mediaName: String? = null,
    @SerialName("media_mime") val mediaMime: String? = null,
    val timestamp: Long = 0L,
)

@Serializable
data class Comment(
    @SerialName("post_id") val postId: String,
    val author: String,
    val text: String,
    val timestamp: Long = 0L,
)

/** First-run connection settings, stored in DataStore (not in git, not hardcoded). */
@Serializable
data class Settings(
    val name: String = "",
    val endpoint: String = "",
    val bucket: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val setupComplete: Boolean = false,
) {
    fun isComplete(): Boolean =
        name.isNotBlank() && endpoint.isNotBlank() && bucket.isNotBlank() &&
            accessKey.isNotBlank() && secretKey.isNotBlank()
}
