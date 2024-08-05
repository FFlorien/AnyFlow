package be.florien.anyflow.feature.playlist.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.feature.playlist.DeletePlaylistViewModel
import be.florien.anyflow.feature.playlist.NewPlaylistViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.playlist.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistListViewModel : BaseViewModel(), NewPlaylistViewModel, DeletePlaylistViewModel {

    @Inject
    lateinit var playlistRepository: be.florien.anyflow.management.playlist.PlaylistRepository

    @Inject
    lateinit var filtersManager: FiltersManager

    val playlistList: LiveData<PagingData<Playlist>> by lazy {
        playlistRepository.getAllPlaylists().cachedIn(this)
    }
    val selection: LiveData<List<Playlist>> = MutableLiveData(mutableListOf())
    private var isForcingSelectMode = false
    val isInSelectionMode: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(selection) {
            value = isForcingSelectMode || it.isNotEmpty()
        }
    }.distinctUntilChanged()
    val hasSelection: LiveData<Boolean> = selection.map {
        it.isNotEmpty()
    }

    fun isSelected(playlist: Playlist) = selection.value?.contains(playlist) ?: false

    fun toggleSelection(playlist: Playlist) {
        val newList = selection.value?.toMutableList() ?: mutableListOf()
        if (!newList.remove(playlist)) {
            newList.add(playlist)
        }
        selection.mutable.value = newList
    }

    override fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    override fun deletePlaylist() {
        viewModelScope.launch {
            selection.value?.forEach {
                playlistRepository.deletePlaylist(it.id)
            }
            selection.mutable.value = mutableListOf()
        }
    }

    fun toggleSelectionMode() {
        isForcingSelectMode = isInSelectionMode.value == false
        isInSelectionMode.mutable.value = if (isForcingSelectMode) {
            true
        } else {
            selection.mutable.value = mutableListOf()
            false
        }
    }

    fun filterOnSelection() {
        filtersManager.clearFilters()
        selection.value?.forEach {
            filtersManager.addFilter(
                be.florien.anyflow.management.filters.model.Filter(
                    be.florien.anyflow.management.filters.model.Filter.FilterType.PLAYLIST_IS,
                    it.id,
                    it.name
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            filtersManager.commitChanges()
        }
    }
}