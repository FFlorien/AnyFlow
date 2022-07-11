package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel() {
    var playlistId: Long = -1

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var ampacheDataSource: AmpacheDataSource

    val selectionList: LiveData<List<Long>> = MutableLiveData(listOf())

    val songList: LiveData<PagingData<SongInfo>>
        get() = dataRepository.getPlaylistSongs(playlistId) { it.toViewSongInfo() }

    fun getCover(long: Long) = ampacheDataSource.getAlbumArtUrl(long)

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
}