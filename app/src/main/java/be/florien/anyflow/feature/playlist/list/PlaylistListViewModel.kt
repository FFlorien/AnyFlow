package be.florien.anyflow.feature.playlist.list

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.playlist.DeletePlaylistViewModel
import be.florien.anyflow.feature.playlist.NewPlaylistViewModel
import be.florien.anyflow.feature.playlist.PlaylistRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistListViewModel : BaseViewModel(), NewPlaylistViewModel, DeletePlaylistViewModel {

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    val playlistList: LiveData<PagingData<Playlist>> by lazy {
        playlistRepository.getAllPlaylists().cachedIn(this)
    }
    val selection: LiveData<List<Long>> = MutableLiveData(mutableListOf())
    private var isForcingSelectMode = false
    val isInSelectionMode: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(selection) {
            value = isForcingSelectMode || it.isNotEmpty()
        }
    }.distinctUntilChanged()
    val hasSelection: LiveData<Boolean> = Transformations.map(selection) {
        it.isNotEmpty()
    }

    fun isSelected(id: Long) = selection.value?.contains(id) ?: false

    fun toggleSelection(id: Long) {
        val newList = selection.value?.toMutableList() ?: mutableListOf()
        if (!newList.remove(id)) {
            newList.add(id)
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
                playlistRepository.deletePlaylist(it)
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
}