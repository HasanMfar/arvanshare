package com.arvanshare.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvanshare.app.data.ArvanRepository
import com.arvanshare.app.data.Comment
import com.arvanshare.app.data.Post
import com.arvanshare.app.data.Settings
import com.arvanshare.app.data.SettingsRepository
import com.arvanshare.app.db.AppDatabase
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).postDao()
    private val settingsRepo = SettingsRepository(application)
    private val repo = ArvanRepository(application, dao)

    val settings: StateFlow<Settings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Feed UI reads from Room, so it renders instantly (and offline) from cache. */
    val cachedPosts: StateFlow<List<Post>> = dao
        .observeAll()
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

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun saveSettings(s: Settings) {
        viewModelScope.launch { settingsRepo.save(s) }
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

    fun addComment(text: String) {
        val current = _detail.value ?: return
        viewModelScope.launch {
            _busy.value = true
            repo.addComment(settings.value, current.post.postId, text)
                .onSuccess { loadDetail(current.post) }
                .onFailure { _snackbar.value = it.message ?: "Failed to post comment" }
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
