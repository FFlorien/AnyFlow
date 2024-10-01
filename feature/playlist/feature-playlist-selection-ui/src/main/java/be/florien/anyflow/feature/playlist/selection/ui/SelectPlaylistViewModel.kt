package be.florien.anyflow.feature.playlist.selection.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.common.ui.component.NewPlaylistViewModel
import be.florien.anyflow.feature.playlist.selection.domain.TagType
import be.florien.anyflow.feature.playlist.selection.domain.toViewFilterType
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.management.playlist.model.PlaylistWithPresence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : BaseViewModel(), NewPlaylistViewModel {

    enum class PlaylistAction {
        NONE, DELETION, ADDITION
    }

    class PlaylistWithAction(val playlist: PlaylistWithPresence, val action: PlaylistAction)

    private val actions = mutableMapOf<Long, PlaylistAction>()
    lateinit var playlists: LiveData<List<PlaylistWithPresence>>
    val values: LiveData<List<PlaylistWithAction>> = MediatorLiveData(emptyList())
    val filterCount: LiveData<Int> = MutableLiveData()
    val isCreating: LiveData<Boolean> = MutableLiveData(false)
    val progressLiveData: LiveData<ModificationProgress?> =
        MutableLiveData(null)

    private var id: Long = 0L
    private var filterType: Filter.FilterType = Filter.FilterType.SONG_IS
    private var secondId: Int = -1
    private val filter by lazy {
        if (filterType == Filter.FilterType.DISK_IS) {
            Filter(
                Filter.FilterType.ALBUM_IS,
                id,
                "",
                listOf(
                    Filter(
                        filterType,
                        secondId.toLong(),
                        ""
                    )
                )
            )
        } else {
            Filter(filterType, id, "")
        }
    }

    suspend fun initViewModel(id: Long, type: TagType, secondId: Int) {
        this.id = id
        this.filterType = type.toViewFilterType()
        this.secondId = secondId
        withContext(Dispatchers.IO) {
            filterCount.mutable.postValue(playlistRepository.getSongCountForFilter(filter))
            playlists = playlistRepository.getPlaylistsWithPresence(filter)
            (values as MediatorLiveData).addSource(playlists) { _ ->
                updateValues()
            }
        }
    }

    fun rotateActionForPlaylist(selectionValue: Long) {
        val playlist = playlists.value?.first { it.id == selectionValue } ?: return
        val currentAction = actions[selectionValue] ?: PlaylistAction.NONE

        val nextAction =
            if (currentAction == PlaylistAction.NONE) { // this all if else is ugly but I'm tired
                if (playlist.presence < (filterCount.value ?: 0)) {
                    PlaylistAction.ADDITION
                } else if (playlist.presence > 0) {
                    PlaylistAction.DELETION
                } else {
                    PlaylistAction.NONE
                }
            } else if (currentAction == PlaylistAction.ADDITION) {
                if (playlist.presence > 0) {
                    PlaylistAction.DELETION
                } else {
                    PlaylistAction.NONE
                }
            } else {
                PlaylistAction.NONE
            }

        actions[selectionValue] = nextAction
        updateValues()
    }

    fun confirmChanges() {
        viewModelScope.launch {
            val confirmedAction = actions.filterNot { it.value == PlaylistAction.NONE }
            if (confirmedAction.isEmpty()) {
                progressLiveData.mutable.value = ModificationProgress.Cancelled
                return@launch
            }

            var index = 0
            fun updateProgress() {
                progressLiveData.mutable.value =
                    ModificationProgress.InModificationProgress(index, confirmedAction.size)
                index++
            }
            updateProgress()
            for (playlistId in confirmedAction.filter { it.value == PlaylistAction.ADDITION }.keys) {
                playlistRepository.addSongsToPlaylist(filter, playlistId)
                updateProgress()
            }
            for (playlistId in confirmedAction.filter { it.value == PlaylistAction.DELETION }.keys) {
                playlistRepository.removeSongsFromPlaylist(filter, playlistId)
                updateProgress()
            }
            progressLiveData.mutable.value = ModificationProgress.Finished
        }
    }

    fun getNewPlaylistName() {
        isCreating.mutable.value = true
    }

    private fun updateValues() {
        values.mutable.value = playlists.value?.map { playlist ->
            PlaylistWithAction(playlist, actions[playlist.id] ?: PlaylistAction.NONE)
        }
    }

    override fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    sealed interface ModificationProgress {
        data class InModificationProgress(
            val playlistIndex: Int,
            val playlistCount: Int
        ) : ModificationProgress

        data object Finished : ModificationProgress
        data object Cancelled : ModificationProgress
    }
}