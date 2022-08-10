package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.*
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel(), RemoveSongsViewModel {
    var playlistId: Long = -1

    @Inject
    lateinit var dataRepository: DataRepository

    val selectionList: LiveData<List<Long>> = MutableLiveData(listOf())
    val hasSelection: LiveData<Boolean> = Transformations.map(selectionList) {
        it.isNotEmpty()
    }.distinctUntilChanged()

    val songList: LiveData<PagingData<SongInfo>>
        get() = dataRepository.getPlaylistSongs(playlistId) { it.toViewSongInfo() }

    fun getCover(long: Long) = dataRepository.getAlbumArtUrl(long)

    fun isSelected(id: Long?) = selectionList.value?.contains(id) ?: false

    fun toggleSelection(id: Long?) {
        if (id == null) {
            return
        }
        val newList = selectionList.value?.toMutableList() ?: mutableListOf()
        if (!newList.remove(id)) {
            newList.add(id)
        }
        selectionList.mutable.value = newList
    }

    override fun removeSongs() {
        viewModelScope.launch {
            selectionList.value?.forEach {
                dataRepository.removeSongFromPlaylist(playlistId, it)
            }
            selectionList.mutable.value = mutableListOf()
        }
    }
}