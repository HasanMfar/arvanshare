package com.arvanshare.app.data

import android.content.Context
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ObjectMetadata
import com.arvanshare.app.db.PostDao
import com.arvanshare.app.db.PostEntity
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * All reads/writes against the shared bucket. Every call is a blocking AWS SDK
 * call, so all functions run on Dispatchers.IO.
 *
 * After a refresh, posts are upserted into Room; the feed UI reads from Room so
 * reopening the app shows the cached feed instantly, even while offline.
 * TODO(spec 6.2): only fetch posts newer than the newest cached one (by filename
 * timestamp) instead of re-listing/re-downloading everything on every refresh.
 */
class ArvanRepository(
    private val context: Context,
    private val dao: PostDao,
) {
    private val mediaCache = MediaCache(context)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun testConnection(s: Settings): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = S3ClientFactory.create(s)
            client.listObjectsV2(
                ListObjectsV2Request().apply {
                    bucketName = s.bucket
                    prefix = Keys.POST_PREFIX
                    maxKeys = 1
                }
            )
        }.map { }
    }

    /** Lists post JSONs from the bucket (paged via continuation token) and caches them. */
    suspend fun refreshFeed(s: Settings): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = S3ClientFactory.create(s)
            val posts = mutableListOf<Post>()
            var token: String? = null
            do {
                val result = client.listObjectsV2(
                    ListObjectsV2Request().apply {
                        bucketName = s.bucket
                        prefix = Keys.POST_PREFIX
                        maxKeys = 200
                        continuationToken = token
                    }
                )
                for (summary in result.objectSummaries) {
                    val key = summary.key ?: continue
                    if (!Keys.isPostJsonKey(key)) continue
                    val post = fetchJson<Post>(client, s.bucket, key) ?: continue
                    posts += post
                    dao.upsert(PostEntity.from(post))
                }
                token = result.nextContinuationToken
            } while (token != null && result.isTruncated)
            posts.sortedByDescending { it.timestamp }
        }
    }

    suspend fun uploadPost(
        s: Settings,
        text: String,
        mediaFile: File?,
        mimeType: String? = null,
        originalName: String? = null,
    ): Result<Post> = withContext(Dispatchers.IO) {
        runCatching {
            val client = S3ClientFactory.create(s)
            val postId = Keys.makePostId(s.name)
            var mediaUrl: String? = null
            var mediaType: String? = null
            var mediaName: String? = null
            var mediaMime: String? = null

            // Media first, so the post JSON never references a missing object.
            if (mediaFile != null) {
                val name = originalName ?: mediaFile.name
                val mime = mimeType ?: guessMime(name)
                val key = Keys.postMediaKey(postId, name)
                client.putObject(
                    s.bucket, key,
                    FileInputStream(mediaFile),
                    ObjectMetadata().apply {
                        contentType = mime
                        contentLength = mediaFile.length()
                    },
                )
                mediaUrl = key
                mediaType = if (mime.startsWith("image/")) "image" else "file"
                mediaName = name
                mediaMime = mime
            }

            val post = Post(
                postId = postId,
                author = s.name,
                text = text,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                mediaName = mediaName,
                mediaMime = mediaMime,
                timestamp = System.currentTimeMillis() / 1000L,
            )
            val postBody = json.encodeToString(Post.serializer(), post).toByteArray()
            client.putObject(
                s.bucket, Keys.postJsonKey(postId),
                ByteArrayInputStream(postBody),
                ObjectMetadata().apply {
                    contentType = "application/json"
                    contentLength = postBody.size.toLong()
                },
            )
            dao.upsert(PostEntity.from(post))
            post
        }
    }

    /** Best-effort MIME guess from the filename when the picker gives no type. */
    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        "mp3" -> "audio/mpeg"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        else -> "application/octet-stream"
    }

    suspend fun toggleLike(s: Settings, postId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = S3ClientFactory.create(s)
                val key = Keys.likeKey(postId, s.name)
                if (client.doesObjectExist(s.bucket, key)) {
                    client.deleteObject(s.bucket, key)
                    false
                } else {
                    client.putObject(s.bucket, key, ByteArrayInputStream(ByteArray(0)), ObjectMetadata())
                    true
                }
            }
        }

    suspend fun likes(s: Settings, postId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = S3ClientFactory.create(s)
                val folder = Keys.commentsFolder(postId)
                listKeys(client, s.bucket, folder).mapNotNull { key ->
                    val basename = key.removePrefix(folder)
                    if (basename.startsWith("like_") && basename.endsWith(".txt")) {
                        basename.removePrefix("like_").removeSuffix(".txt")
                    } else null
                }.sorted()
            }
        }

    suspend fun comments(s: Settings, postId: String): Result<List<Comment>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = S3ClientFactory.create(s)
                val folder = Keys.commentsFolder(postId)
                listKeys(client, s.bucket, folder).mapNotNull { key ->
                    val basename = key.removePrefix(folder)
                    if (basename.startsWith("like_") || basename.endsWith(".txt")) null
                    else fetchJson<Comment>(client, s.bucket, key)
                }.sortedBy { it.timestamp }
            }
        }

    suspend fun addComment(s: Settings, postId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = S3ClientFactory.create(s)
                val comment = Comment(
                    postId = postId,
                    author = s.name,
                    text = text,
                    timestamp = System.currentTimeMillis() / 1000L,
                )
                val commentBody = json.encodeToString(Comment.serializer(), comment).toByteArray()
                client.putObject(
                    s.bucket, Keys.commentKey(postId, s.name, Keys.nowStamp()),
                    ByteArrayInputStream(commentBody),
                    ObjectMetadata().apply {
                        contentType = "application/json"
                        contentLength = commentBody.size.toLong()
                    },
                )
                Unit
            }
        }

    /** Downloads media to the cache (via authenticated S3) and returns the local file. */
    suspend fun mediaFile(s: Settings, post: Post): File? = withContext(Dispatchers.IO) {
        val key = post.mediaUrl ?: return@withContext null
        mediaCache.get(key)?.let { return@withContext it }
        runCatching {
            val client = S3ClientFactory.create(s)
            val obj = client.getObject(GetObjectRequest(s.bucket, key))
            val target = mediaCache.fileFor(key)
            obj.objectContent.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            target
        }.getOrNull()
    }

    private inline fun <reified T> fetchJson(client: AmazonS3Client, bucket: String, key: String): T? =
        runCatching {
            val body = client.getObject(GetObjectRequest(bucket, key)).objectContent
            json.decodeFromString<T>(body.use { it.readBytes().toString(Charsets.UTF_8) })
        }.getOrNull()

    private fun listKeys(client: AmazonS3Client, bucket: String, folder: String): List<String> =
        client.listObjectsV2(
            ListObjectsV2Request().apply {
                bucketName = bucket
                prefix = folder
            }
        ).objectSummaries.mapNotNull { it.key }
}
