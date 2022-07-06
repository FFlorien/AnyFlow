package be.florien.anyflow.feature.playlist.songs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.SongInfo
import javax.inject.Inject

class PlaylistSongsViewModel : ViewModel() {
    var playlistId: Long = -1

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var ampacheConnection: AmpacheConnection

    val songList: LiveData<PagingData<SongInfo>>
        get() = dataRepository.getPlaylistSongs(playlistId) { it.toViewSongInfo() }

    fun getCover(long: Long) = ampacheConnection.getAlbumArtUrl(long)
}