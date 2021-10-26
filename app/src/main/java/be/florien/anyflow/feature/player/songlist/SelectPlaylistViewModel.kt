package be.florien.anyflow.feature.player.songlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectPlaylistViewModel @Inject constructor(val dataRepository: DataRepository) : BaseViewModel() {

    private val currentSelection: MutableSet<SelectionItem> = mutableSetOf()
    val currentSelectionLive: LiveData<Set<SelectionItem>> = MutableLiveData(setOf())

    val values: LiveData<PagingData<SelectionItem>> = dataRepository.getPlaylists(::convert)
    val isFinished: LiveData<Boolean> = MutableLiveData(false)

    private fun convert(playlist: DbPlaylist) =
            SelectionItem(playlist.id, playlist.name, currentSelection.any {
                it.id == playlist.id
            })

    fun changeFilterSelection(selectionValue: SelectionItem) {
        if (!currentSelection.remove(selectionValue)) {
            currentSelection.add(selectionValue)
        }
        currentSelectionLive.mutable.value = currentSelection
    }

    fun confirmChanges(songId: Long) {
        viewModelScope.launch {
            for (playlistId in currentSelection.map { it.id }) {
                dataRepository.addSongToPlaylist(songId, playlistId)
            }
            isFinished.mutable.value = true
        }
    }

    class SelectionItem(
            val id: Long,
            val displayName: String,
            var isSelected: Boolean
    )
}