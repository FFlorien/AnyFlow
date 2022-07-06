package be.florien.anyflow.feature.playlist.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.toViewPlaylist
import be.florien.anyflow.data.view.Playlist
import javax.inject.Inject

class PlaylistListViewModel : ViewModel() {

    @Inject
    lateinit var dataRepository: DataRepository

    val playlistList: LiveData<PagingData<Playlist>> by lazy {
        dataRepository.getPlaylists { it.toViewPlaylist() }.cachedIn(this)
    }
}