package be.florien.anyflow.data.server

import be.florien.anyflow.injection.ServerScope
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

/**
 * Manager for the ampache API server-side todo
 */
@ServerScope
open class AmpacheEditSource
@Inject constructor(@Named("authenticated") retrofit: Retrofit) {

    private val ampacheEditApi = retrofit.create(AmpacheEditApi::class.java)

    suspend fun createPlaylist(name: String) {
        ampacheEditApi.createPlaylist(name = name)
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditApi.deletePlaylist(id = id.toString())
    }

    suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>, fromPosition: Int) {
        if (songIds.isEmpty()) {
            return
        } else if (songIds.size > 75) {
            val middleIndex = songIds.size / 2
            addToPlaylist(playlistId, songIds.subList(0, middleIndex), fromPosition)
            addToPlaylist(playlistId, songIds.subList(middleIndex, songIds.size), fromPosition + middleIndex)
        } else {
            ampacheEditApi.editPlaylist(
                playlistId = playlistId.toString(),
                items = songIds.joinToString(","),
                tracks = fromPosition.rangeUntil(fromPosition + songIds.size).joinToString(",")
            )
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        ampacheEditApi.removeFromPlaylist(
            filter = playlistId,
            song = songId
        )
    }
}
