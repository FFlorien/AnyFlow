package be.florien.anyflow.feature.playlist

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.SyncRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.AmpacheEditSource
import be.florien.anyflow.data.toViewPlaylist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.extension.convertToPagingLiveData
import be.florien.anyflow.injection.ServerScope
import javax.inject.Inject

@ServerScope
class PlaylistRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheEditSource: AmpacheEditSource,
    private val urlRepository: UrlRepository,
    private val dataRepository: SyncRepository
) {

    private val queryComposer = QueryComposer()

    fun <T : Any> getPlaylists(
        mapping: (DbPlaylistWithCount) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =

        libraryDatabase.getPlaylistDao().rawQueryPaging(
            queryComposer.getQueryForPlaylistFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun getAllPlaylists(): LiveData<PagingData<Playlist>> =
        getPlaylists({ it.toViewPlaylist(urlRepository.getPlaylistArtUrl(it.id)) }, null, null)

    suspend fun getPlaylistsWithSongPresence(songId: Long): List<Long> =
        libraryDatabase.getPlaylistDao().getPlaylistsWithCountAndSongPresence(songId)

    fun <T : Any> getPlaylistSongs(
        playlistId: Long,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =

        libraryDatabase.getPlaylistSongsDao().songsFromPlaylist(playlistId).map { mapping(it) }
            .convertToPagingLiveData()

    suspend fun <T : Any> getPlaylistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbPlaylistWithCount) -> T
    ): List<T> =
        libraryDatabase.getPlaylistDao().rawQueryListDisplay(
            queryComposer.getQueryForPlaylistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheEditSource.createPlaylist(name)
        dataRepository.playlists()
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditSource.deletePlaylist(id)
        libraryDatabase.getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheEditSource.addSongToPlaylist(songId, playlistId)
        val playlistLastOrder = libraryDatabase.getPlaylistSongsDao().playlistLastOrder(playlistId) ?: -1
        libraryDatabase.getPlaylistSongsDao().upsert(
            listOf(
                DbPlaylistSongs(
                    playlistLastOrder + 1,
                    songId,
                    playlistId
                )
            )
        )
    }

    suspend fun removeSongFromPlaylist(songId: Long, playlistId: Long) {
        ampacheEditSource.removeSongFromPlaylist(playlistId, songId)
        libraryDatabase.getPlaylistSongsDao().delete(DbPlaylistSongs(0, songId, playlistId))
    }
}