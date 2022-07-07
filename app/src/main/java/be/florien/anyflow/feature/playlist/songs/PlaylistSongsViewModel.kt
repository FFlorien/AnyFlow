package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import javax.inject.Inject

class PlaylistSongsViewModel : BaseViewModel() {
    var playlistId: Long = -1

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var ampacheConnection: AmpacheConnection

    val selectionList: LiveData<List<Long>> = MutableLiveData(listOf())

    val songList: LiveData<PagingData<SongInfo>>
        get() = dataRepository.getPlaylistSongs(playlistId) { it.toViewSongInfo() }

    fun getCover(long: Long) = ampacheConnection.getAlbumArtUrl(long)

    fun isSelected(id: Long?) = selectionList.value?.contains(id) ?: false

    fun setSelection(id: Long?, isSelected: Boolean) {
        if (id == null) {
            return
        }
        val newList = selectionList.value?.toMutableList() ?: mutableListOf()
        if (isSelected) {
            newList.add(id)
        } else {
            newList.remove(id)
        }
        selectionList.mutable.value = newList
    }
}