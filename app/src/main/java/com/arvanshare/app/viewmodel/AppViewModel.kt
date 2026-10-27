package com.arvanshare.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvanshare.app.data.ArvanRepository
import com.arvanshare.app.data.BucketProfile
import com.arvanshare.app.data.Comment
import com.arvanshare.app.data.Post
import com.arvanshare.app.data.RoomInfo
import com.arvanshare.app.data.Settings
import com.arvanshare.app.data.SettingsRepository
import com.arvanshare.app.db.AppDatabase
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedState {
    data object Idle : FeedState
    data object Loading : FeedState
    data object Loaded : FeedState
    data class Error(val message: String) : FeedState
}

data class DetailData(
    val post: Post,
    val likes: List<String>,
    val comments: List<Comment>,
    val likedByMe: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).postDao()
    private val settingsRepo = SettingsRepository(application)
    private val repo = ArvanRepository(application, dao)

    val settings: StateFlow<Settings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Feed UI reads from Room, scoped to activeRoom so switching rooms or buckets is clean. */
    val cachedPosts: StateFlow<List<Post>> = settingsRepo.settings
        .flatMapLatest { s -> dao.observeByRoom(s.activeRoom) }
        .map { list -> list.map { it.toPost() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Idle)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    private val _detail = MutableStateFlow<DetailData?>(null)
    val detail: StateFlow<DetailData?> = _detail.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms.asStateFlow()

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun saveSettings(s: Settings) {
        viewModelScope.launch {
            // Ensure the active bucket profile is also synced in the profiles list
            val profiles = s.profiles.toMutableList()
            val activeId = if (s.activeProfileId.isNotBlank()) s.activeProfileId else UUID.randomUUID().toString()
            val existingIndex = profiles.indexOfFirst { it.id == activeId }
            val currentProfile = BucketProfile(
                id = activeId,
                label = if (existingIndex >= 0) profiles[existingIndex].label else s.bucket,
                endpoint = s.endpoint,
                bucket = s.bucket,
                accessKey = s.accessKey,
                secretKey = s.secretKey,
                activeRoom = s.activeRoom,
                activeRoomPassword = s.activeRoomPassword,
            )
            if (existingIndex >= 0) {
                profiles[existingIndex] = currentProfile
            } else {
                profiles.add(currentProfile)
            }
            settingsRepo.save(s.copy(profiles = profiles, activeProfileId = activeId))
        }
    }

    /** Add a new bucket profile and switch to it */
    fun addAndSwitchBucket(profile: BucketProfile) {
        viewModelScope.launch {
            val s = settings.value
            val newProfiles = s.profiles.filter { it.id != profile.id } + profile
            val updated = s.copy(
                endpoint = profile.endpoint,
                bucket = profile.bucket,
                accessKey = profile.accessKey,
                secretKey = profile.secretKey,
                activeRoom = profile.activeRoom,
                activeRoomPassword = profile.activeRoomPassword,
                profiles = newProfiles,
                activeProfileId = profile.id,
            )
            settingsRepo.save(updated)
            dao.clearAll()
            refreshFeed()
        }
    }

    /** Switch active bucket to an existing profile */
    fun switchBucketProfile(profileId: String) {
        viewModelScope.launch {
            val s = settings.value
            val target = s.profiles.find { it.id == profileId } ?: return@launch
            val updated = s.copy(
                endpoint = target.endpoint,
                bucket = target.bucket,
                accessKey = target.accessKey,
                secretKey = target.secretKey,
                activeRoom = target.activeRoom,
                activeRoomPassword = target.activeRoomPassword,
                activeProfileId = target.id,
            )
            settingsRepo.save(updated)
            dao.clearAll()
            refreshFeed()
        }
    }

    /** Delete a bucket profile and rebind the active selection to a remaining profile when needed. */
    fun deleteBucketProfile(profileId: String) {
        viewModelScope.launch {
            val s = settings.value
            val remainingProfiles = s.profiles.filter { it.id != profileId }

            val updated = if (s.activeProfileId == profileId) {
                val nextProfile = remainingProfiles.firstOrNull()
                if (nextProfile == null) {
                    s.copy(
                        profiles = remainingProfiles,
                        activeProfileId = "",
                    )
                } else {
                    s.copy(
                        endpoint = nextProfile.endpoint,
                        bucket = nextProfile.bucket,
                        accessKey = nextProfile.accessKey,
                        secretKey = nextProfile.secretKey,
                        activeRoom = nextProfile.activeRoom,
                        activeRoomPassword = nextProfile.activeRoomPassword,
                        profiles = remainingProfiles,
                        activeProfileId = nextProfile.id,
                    )
                }
            } else {
                s.copy(profiles = remainingProfiles)
            }

            settingsRepo.save(updated)

            if (s.activeProfileId == profileId) {
                dao.clearAll()
                refreshFeed()
            }
        }
    }

    /** Load all rooms available in the current bucket */
    fun loadRooms() {
        viewModelScope.launch {
            val s = settings.value
            if (!s.isComplete()) return@launch
            repo.listRooms(s).onSuccess { list ->
                _rooms.value = list
            }
        }
    }

    /** Switch room with password verification or creation */
    fun switchRoom(room: String, password: String = "", onDone: (String?) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            val s = settings.value
            val result = repo.verifyOrCreateRoom(s, room, password)
            _busy.value = false
            if (result.isSuccess) {
                val updated = s.copy(
                    activeRoom = room.trim(),
                    activeRoomPassword = password.trim(),
                )
                settingsRepo.save(updated)
                refreshFeed()
                loadRooms()
                onDone(null)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Failed to switch room"
                _snackbar.value = errMsg
                onDone(errMsg)
            }
        }
    }

    /** Tests the connection using the draft settings (not yet saved to DataStore). */
    fun testConnection(s: Settings, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val result = repo.testConnection(s)
            _busy.value = false
            onDone(result.exceptionOrNull()?.message)
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            repo.refreshFeed(settings.value)
                .onSuccess { _feedState.value = FeedState.Loaded }
                .onFailure {
                    _feedState.value = FeedState.Error(it.message ?: "Failed to load feed")
                }
        }
    }

    /** onDone(null) means success; otherwise the error message. */
    fun composePost(
        text: String,
        mediaFile: File?,
        mimeType: String? = null,
        originalName: String? = null,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val result = repo.uploadPost(settings.value, text, mediaFile, mimeType, originalName)
            _busy.value = false
            if (result.isSuccess) {
                refreshFeed()
                onDone(null)
            } else {
                onDone(result.exceptionOrNull()?.message ?: "Failed to post")
            }
        }
    }

    fun openDetail(post: Post) {
        viewModelScope.launch { loadDetail(post) }
    }

    fun refreshDetail() {
        val current = _detail.value ?: return
        viewModelScope.launch { loadDetail(current.post) }
    }

    fun toggleLike() {
        val current = _detail.value ?: return
        viewModelScope.launch {
            repo.toggleLike(settings.value, current.post.postId)
                .onSuccess { loadDetail(current.post) }
                .onFailure { _snackbar.value = it.message ?: "Failed to update like" }
        }
    }

    /** onDone gets true when the comment was actually posted to S3. */
    fun addComment(text: String, onDone: (Boolean) -> Unit = {}) {
        val current = _detail.value ?: run { onDone(false); return }
        viewModelScope.launch {
            _busy.value = true
            repo.addComment(settings.value, current.post.postId, text)
                .onSuccess { loadDetail(current.post); onDone(true) }
                .onFailure {
                    _snackbar.value = it.message ?: "Failed to post comment"
                    onDone(false)
                }
            _busy.value = false
        }
    }

    suspend fun mediaFile(post: Post): File? = repo.mediaFile(settings.value, post)

    private suspend fun loadDetail(post: Post) {
        val s = settings.value
        val likes = repo.likes(s, post.postId).getOrDefault(emptyList())
        val comments = repo.comments(s, post.postId).getOrDefault(emptyList())
        _detail.value = DetailData(post, likes, comments, s.name in likes)
    }
}
