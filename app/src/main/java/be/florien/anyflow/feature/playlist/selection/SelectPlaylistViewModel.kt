package be.florien.anyflow.feature.playlist.selection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.playlist.NewPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectPlaylistViewModel @Inject constructor(val dataRepository: DataRepository) : BaseViewModel(), NewPlaylistViewModel {

    private val currentSelection: MutableSet<Long> = mutableSetOf()
    val currentSelectionLive: LiveData<Set<Long>> = MutableLiveData(setOf())

    val values: LiveData<PagingData<Playlist>> = dataRepository.getAllPlaylists().cachedIn(this)
    val isCreating: LiveData<Boolean> = MutableLiveData(false)
    val isFinished: LiveData<Boolean> = MutableLiveData(false)

    suspend fun updateSelectionWithPlaylists(songId: Long) = withContext(Dispatchers.IO) {
        currentSelection.addAll(dataRepository.getPlaylistsWithSongPresence(songId))
        withContext(Dispatchers.Main) {
            currentSelectionLive.mutable.value = currentSelection
        }
    }

    fun toggleSelection(selectionValue: Long) {
        if (!currentSelection.remove(selectionValue)) {
            currentSelection.add(selectionValue)
        }
        currentSelectionLive.mutable.value = currentSelection.toSet()
    }

    fun isSelected(id: Long) = currentSelection.contains(id)

    fun confirmChanges(songId: Long) {
        viewModelScope.launch {
            val playlistWithSong = dataRepository.getPlaylistsWithSongPresence(songId)
            for (playlistId in currentSelection) {
                if (!playlistWithSong.contains(playlistId)) {
                    dataRepository.addSongToPlaylist(songId, playlistId)
                }
            }
            for (playlistId in playlistWithSong) {
                if (!currentSelection.contains(playlistId)) {
                    dataRepository.removeSongFromPlaylist(songId, playlistId)
                }
            }
            isFinished.mutable.value = true
        }
    }

    fun getNewPlaylistName() {
        isCreating.mutable.value = true
    }

    override fun createPlaylist(name: String) {
        viewModelScope.launch {
            dataRepository.createPlaylist(name)
        }
    }
}