package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.toViewSongDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.playlist.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel(), RemoveSongsViewModel {
    lateinit var playlist: Playlist

    @Inject
    lateinit var urlRepository: UrlRepository

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var filtersManager: FiltersManager

    val selectionList: LiveData<List<Long>> = MutableLiveData(listOf())
    val hasSelection: LiveData<Boolean> = selectionList.map {
        it.isNotEmpty()
    }.distinctUntilChanged()

    val songList: LiveData<PagingData<SongDisplay>>
        get() = playlistRepository.getPlaylistSongs(playlist.id) { it.toViewSongDisplay() }

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
                playlistRepository.removeSongFromPlaylist(it, playlist.id)
            }
            selectionList.mutable.value = mutableListOf()
        }
    }

    fun filterOnPlaylist() {
        filtersManager.clearFilters()
        filtersManager.addFilter(Filter(Filter.FilterType.PLAYLIST_IS, playlist.id, playlist.name))
        viewModelScope.launch(Dispatchers.IO) {
            filtersManager.commitChanges()
        }
    }
}