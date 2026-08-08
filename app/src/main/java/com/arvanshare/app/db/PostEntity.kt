package com.arvanshare.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arvanshare.app.data.Post

/** Local cache of posts so the feed opens instantly and only new posts are fetched. */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val postId: String,
    val author: String,
    val text: String,
    val mediaUrl: String?,
    val mediaType: String?,
    val mediaName: String?,
    val mediaMime: String?,
    val timestamp: Long,
) {
    fun toPost() = Post(
        postId = postId,
        author = author,
        text = text,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        mediaName = mediaName,
        mediaMime = mediaMime,
        timestamp = timestamp,
    )

    companion object {
        fun from(p: Post) = PostEntity(
            postId = p.postId,
            author = p.author,
            text = p.text,
            mediaUrl = p.mediaUrl,
            mediaType = p.mediaType,
            mediaName = p.mediaName,
            mediaMime = p.mediaMime,
            timestamp = p.timestamp,
        )
    }
}
