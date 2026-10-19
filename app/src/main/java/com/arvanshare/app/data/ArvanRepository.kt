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
 * Supports root feed (backward compatible) and private rooms with simple password protection.
 * Refreshes are incremental (spec 6.2): one cheap keys-only listing diffs the
 * remote feed against the Room cache, then only unseen post JSONs are
 * downloaded and posts that vanished remotely are dropped from the cache.
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
                    prefix = Keys.postPrefix(s.activeRoom)
                    maxKeys = 1
                }
            )
        }.map { }
    }

    /**
     * Verify password for an existing room or create a new room with an optional password.
     */
    suspend fun verifyOrCreateRoom(
        s: Settings,
        room: String,
        password: String,
    ): Result<RoomInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (Keys.isDefaultRoom(room)) {
                return@runCatching RoomInfo(name = "default", hasPassword = false)
            }
            val client = S3ClientFactory.create(s)
            val key = Keys.roomInfoKey(room)
            val existingInfo = fetchJson<RoomInfo>(client, s.bucket, key)
            if (existingInfo != null) {
                if (existingInfo.hasPassword) {
                    val expectedHash = existingInfo.passwordHash ?: ""
                    val enteredHash = Keys.hashPassword(password)
                    if (expectedHash.isNotEmpty() && expectedHash != enteredHash) {
                        throw IllegalArgumentException("Incorrect password for room '$room'")
                    }
                }
                existingInfo
            } else {
                // Room does not exist yet -> create it
                val info = RoomInfo(
                    name = Keys.sanitizeRoom(room),
                    hasPassword = password.isNotBlank(),
                    passwordHash = if (password.isNotBlank()) Keys.hashPassword(password) else null,
                    createdAt = System.currentTimeMillis() / 1000L,
                    createdBy = s.name,
                )
                val body = json.encodeToString(RoomInfo.serializer(), info).toByteArray()
                client.putObject(
                    s.bucket, key,
                    ByteArrayInputStream(body),
                    ObjectMetadata().apply {
                        contentType = "application/json"
                        contentLength = body.size.toLong()
                    },
                )
                info
            }
        }
    }

    /** Discover all existing rooms in the bucket. */
    suspend fun listRooms(s: Settings): Result<List<RoomInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = S3ClientFactory.create(s)
            val keys = listAllKeys(client, s.bucket, Keys.ROOMS_PREFIX)
            val roomJsonKeys = keys.filter { it.endsWith("/room.json") }
            val rooms = mutableListOf<RoomInfo>()
            for (key in roomJsonKeys) {
                val info = fetchJson<RoomInfo>(client, s.bucket, key)
                if (info != null) {
                    rooms.add(info)
                }
            }
            rooms.sortedBy { it.name }
        }
    }

    /**
     * Syncs the feed for the active room via a keys-only listing: fetches only post JSONs that are
     * not cached yet and evicts cached posts whose JSON disappeared remotely.
     * The returned list comes from Room, sorted newest first.
     */
    suspend fun refreshFeed(s: Settings): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val room = s.activeRoom
            val client = S3ClientFactory.create(s)
            val prefix = Keys.postPrefix(room)
            val remoteKeys = listAllKeys(client, s.bucket, prefix)
            val remoteJsonKeys = remoteKeys.filterTo(mutableSetOf()) { Keys.isPostJsonKey(it, room) }

            // Posts cached locally but deleted remotely -> drop from Room.
            val cachedIds = dao.getAllIdsByRoom(room).toSet()
            for (cachedId in cachedIds) {
                if (Keys.postJsonKey(cachedId, room) !in remoteJsonKeys) {
                    dao.deleteById(cachedId)
                }
            }

            // Post JSONs never fetched before -> download and cache them.
            for (key in remoteJsonKeys) {
                val postId = Keys.postIdFromJsonKey(key, room)
                if (postId in cachedIds) continue
                val post = fetchJson<Post>(client, s.bucket, key) ?: continue
                dao.upsert(PostEntity.from(post, room = room))
            }

            dao.getAllByRoom(room).map { it.toPost() }
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
            val room = s.activeRoom
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
                val key = Keys.postMediaKey(postId, name, room)
                client.putObject(
                    s.bucket, key,
                    FileInputStream(mediaFile),
                    ObjectMetadata().apply {
                        contentType = mime
                        contentLength = mediaFile.length()
                    },
                )
                mediaUrl = key
                mediaType = when {
                    mime.startsWith("image/") -> "image"
                    mime.startsWith("audio/") -> "audio"   // voice notes
                    else -> "file"
                }
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
                room = room,
            )
            val postBody = json.encodeToString(Post.serializer(), post).toByteArray()
            client.putObject(
                s.bucket, Keys.postJsonKey(postId, room),
                ByteArrayInputStream(postBody),
                ObjectMetadata().apply {
                    contentType = "application/json"
                    contentLength = postBody.size.toLong()
                },
            )
            dao.upsert(PostEntity.from(post, room = room))
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
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        else -> "application/octet-stream"
    }

    suspend fun toggleLike(s: Settings, postId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val room = s.activeRoom
                val client = S3ClientFactory.create(s)
                val key = Keys.likeKey(postId, s.name, room)
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
                val room = s.activeRoom
                val client = S3ClientFactory.create(s)
                val folder = Keys.commentsFolder(postId, room)
                listAllKeys(client, s.bucket, folder).mapNotNull { key ->
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
                val room = s.activeRoom
                val client = S3ClientFactory.create(s)
                val folder = Keys.commentsFolder(postId, room)
                listAllKeys(client, s.bucket, folder).mapNotNull { key ->
                    val basename = key.removePrefix(folder)
                    if (basename.startsWith("like_") || basename.endsWith(".txt")) null
                    else fetchJson<Comment>(client, s.bucket, key)
                }.sortedBy { it.timestamp }
            }
        }

    suspend fun addComment(s: Settings, postId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val room = s.activeRoom
                val client = S3ClientFactory.create(s)
                val comment = Comment(
                    postId = postId,
                    author = s.name,
                    text = text,
                    timestamp = System.currentTimeMillis() / 1000L,
                )
                val commentBody = json.encodeToString(Comment.serializer(), comment).toByteArray()
                client.putObject(
                    s.bucket, Keys.commentKey(postId, s.name, Keys.nowStamp(), room),
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
            mediaCache.trim()
            target
        }.getOrNull()
    }

    /** Keys-only listing with pagination (single calls cap at 1000 items). */
    private fun listAllKeys(client: AmazonS3Client, bucket: String, prefix: String): List<String> {
        val keys = mutableListOf<String>()
        var token: String? = null
        do {
            val result = client.listObjectsV2(
                ListObjectsV2Request().apply {
                    bucketName = bucket
                    this.prefix = prefix
                    maxKeys = 1000
                    continuationToken = token
                }
            )
            for (summary in result.objectSummaries) {
                summary.key?.let { keys += it }
            }
            token = if (result.isTruncated) result.nextContinuationToken else null
        } while (token != null)
        return keys
    }

    private inline fun <reified T> fetchJson(client: AmazonS3Client, bucket: String, key: String): T? =
        runCatching {
            val body = client.getObject(GetObjectRequest(bucket, key)).objectContent
            json.decodeFromString<T>(body.use { it.readBytes().toString(Charsets.UTF_8) })
        }.getOrNull()
}
