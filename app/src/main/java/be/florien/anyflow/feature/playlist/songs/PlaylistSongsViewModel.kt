package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.*
import androidx.paging.PagingData
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.toViewSongDisplay
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.playlist.PlaylistRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel(), RemoveSongsViewModel {
    var playlistId: Long = -1

    @Inject
    lateinit var urlRepository: UrlRepository

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    val selectionList: LiveData<List<Long>> = MutableLiveData(listOf())
    val hasSelection: LiveData<Boolean> = Transformations.map(selectionList) {
        it.isNotEmpty()
    }.distinctUntilChanged()

    val songList: LiveData<PagingData<SongDisplay>>
        get() = playlistRepository.getPlaylistSongs(playlistId) { it.toViewSongDisplay() }

    fun getCover(long: Long) = urlRepository.getAlbumArtUrl(long)

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
                playlistRepository.removeSongFromPlaylist(it, playlistId)
            }
            selectionList.mutable.value = mutableListOf()
        }
    }
}