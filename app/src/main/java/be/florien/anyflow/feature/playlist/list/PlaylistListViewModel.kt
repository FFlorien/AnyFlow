package be.florien.anyflow.feature.playlist.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.toViewPlaylist
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.playlist.NewPlaylistViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistListViewModel : BaseViewModel(), NewPlaylistViewModel {

    @Inject
    lateinit var dataRepository: DataRepository

    val playlistList: LiveData<PagingData<Playlist>> by lazy {
        dataRepository.getPlaylists { it.toViewPlaylist() }.cachedIn(this)
    }

    val selection: LiveData<List<Long>> = MutableLiveData(mutableListOf())

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
            dataRepository.createPlaylist(name)
        }
    }
}