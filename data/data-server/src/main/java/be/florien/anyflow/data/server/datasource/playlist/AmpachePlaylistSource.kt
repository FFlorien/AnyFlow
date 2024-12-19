package be.florien.anyflow.data.server.datasource.playlist

import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.data.server.toNetResult
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

@ServerScope
open class AmpachePlaylistSource
@Inject constructor(@Named("authenticated") retrofit: Retrofit) {

    private val ampachePlaylistApi = retrofit.create(AmpachePlaylistApi::class.java)

    suspend fun createPlaylist(name: String) {
        ampachePlaylistApi.createPlaylist(name = name)
    }

    suspend fun addToPlaylist(playlistId: Long, songId: Long) = ampachePlaylistApi
        .addToPlaylist(
            playlistId = playlistId.toString(),
            item = songId.toString(),
            type = "song"
        )
        .toNetResult()

    suspend fun addToPlaylist(
        playlistId: Long,
        songIds: List<Long>,
        fromPosition: Int
    ) { //todo handle responses
        if (songIds.isEmpty()) {
            return
        } else if (songIds.size > 75) {//todo magic number
            val middleIndex = songIds.size / 2
            addToPlaylist(playlistId, songIds.subList(0, middleIndex), fromPosition)
            addToPlaylist(
                playlistId,
                songIds.subList(middleIndex, songIds.size),
                fromPosition + middleIndex
            )
        } else {
            ampachePlaylistApi.editPlaylist(
                playlistId = playlistId.toString(),
                items = songIds.joinToString(","),
                tracks = fromPosition.rangeUntil(fromPosition + songIds.size).joinToString(",")
            )
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = ampachePlaylistApi
        .removeFromPlaylist(
            filter = playlistId,
            song = songId
        )
        .toNetResult()

    suspend fun deletePlaylist(id: Long) = ampachePlaylistApi
        .deletePlaylist(id = id.toString())
        .toNetResult()
}
