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
    val room: String = "",
)

@Serializable
data class Comment(
    @SerialName("post_id") val postId: String,
    val author: String,
    val text: String,
    val timestamp: Long = 0L,
)

@Serializable
data class RoomInfo(
    val name: String = "",
    @SerialName("has_password") val hasPassword: Boolean = false,
    @SerialName("password_hash") val passwordHash: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("created_by") val createdBy: String = "",
)

@Serializable
data class BucketProfile(
    val id: String = "",
    val label: String = "",
    val endpoint: String = "",
    val bucket: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val activeRoom: String = "",
    val activeRoomPassword: String = "",
)

/** First-run and ongoing connection settings, stored in DataStore. */
@Serializable
data class Settings(
    val name: String = "",
    val endpoint: String = "",
    val bucket: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val setupComplete: Boolean = false,
    val activeRoom: String = "",
    val activeRoomPassword: String = "",
    val profiles: List<BucketProfile> = emptyList(),
    val activeProfileId: String = "",
) {
    fun isComplete(): Boolean =
        name.isNotBlank() && endpoint.isNotBlank() && bucket.isNotBlank() &&
            accessKey.isNotBlank() && secretKey.isNotBlank()
}
