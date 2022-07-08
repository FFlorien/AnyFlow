package be.florien.anyflow.feature.player.songlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectPlaylistViewModel @Inject constructor(val dataRepository: DataRepository) : BaseViewModel() {

    private val currentSelection: MutableSet<Long> = mutableSetOf()
    val currentSelectionLive: LiveData<Set<Long>> = MutableLiveData(setOf())

    val values: LiveData<PagingData<Playlist>> = dataRepository.getPlaylists().cachedIn(this)
    val isCreating: LiveData<Boolean> = MutableLiveData(false)
    val isFinished: LiveData<Boolean> = MutableLiveData(false)

    fun changeSelection(selectionValue: Long, isSelected: Boolean) {
        if (isSelected) {
            currentSelection.add(selectionValue)
        } else {
            currentSelection.remove(selectionValue)
        }
        currentSelectionLive.mutable.value = currentSelection
    }

    fun isSelected(id: Long) = currentSelection.contains(id)

    fun confirmChanges(songId: Long) {
        viewModelScope.launch {
            for (playlistId in currentSelection) {
                dataRepository.addSongToPlaylist(songId, playlistId)
            }
            isFinished.mutable.value = true
        }
    }

    fun getNewPlaylistName() {
        isCreating.mutable.value = true
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            dataRepository.createPlaylist(name)
        }
    }
}