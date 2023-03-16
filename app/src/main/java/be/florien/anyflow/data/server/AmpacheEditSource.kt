package be.florien.anyflow.data.server

import be.florien.anyflow.injection.ServerScope
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Manager for the ampache API server-side todo
 */
@ServerScope
open class AmpacheEditSource
@Inject constructor(retrofit: Retrofit) {

    private val ampacheEditApi = retrofit.create(AmpacheEditApi::class.java)

    suspend fun createPlaylist(name: String) {
        ampacheEditApi.createPlaylist(name = name)
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditApi.deletePlaylist(id = id.toString())
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheEditApi.addToPlaylist(
            filter = playlistId,
            songId = songId
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        ampacheEditApi.removeFromPlaylist(
            filter = playlistId,
            song = songId
        )
    }
}
