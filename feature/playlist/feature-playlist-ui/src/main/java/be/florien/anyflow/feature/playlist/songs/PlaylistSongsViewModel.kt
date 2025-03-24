package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.management.playlist.model.PlaylistSong
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel(), RemoveSongsViewModel { //todo maybe have a domain specific repository for this module
    lateinit var playlist: PlaylistWithCount

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

    val songList: LiveData<PagingData<PlaylistSong>>
        get() = playlistRepository.getPlaylistSongs(playlist.id)

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
            selectionList.value?.let {
                playlistRepository.removeSongsFromPlaylist(playlist.id, it)
            }
            selectionList.mutable.value = mutableListOf()
        }
    }

    fun filterOnPlaylist() {
        filtersManager.clearFilters()
        filtersManager.addFilter(
            Filter(FilterParam(
                TagFilterType.PLAYLIST_IS,
                playlist.id,
                playlist.name
            ))
        )
        viewModelScope.launch(Dispatchers.IO) {
            filtersManager.commitChanges()
        }
    }
}