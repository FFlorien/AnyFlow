package be.florien.anyflow.feature.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.data.local.model.DbPlaylistSongs
import be.florien.anyflow.data.local.model.DbSongDisplay
import be.florien.anyflow.data.server.AmpacheEditSource
import be.florien.anyflow.data.toViewPlaylist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.data.view.PlaylistWithPresence
import be.florien.anyflow.extension.convertToPagingLiveData
import be.florien.anyflow.feature.sync.SyncRepository
import be.florien.anyflow.injection.ServerScope
import javax.inject.Inject

@ServerScope
class PlaylistRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheEditSource: AmpacheEditSource,
    private val urlRepository: UrlRepository,
    private val syncRepository: SyncRepository
) {

    private val queryComposer = QueryComposer()

    fun <T : Any> getPlaylists(
        mapping: (DbPlaylist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getPlaylistDao().rawQueryPaging(
            queryComposer.getQueryForPlaylistFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun getAllPlaylists(): LiveData<PagingData<Playlist>> =
        libraryDatabase.getPlaylistDao().rawQueryPagingWithCount(
            queryComposer.getQueryForPlaylistWithCountFiltered(null, null)
        ).map { it.toViewPlaylist(urlRepository.getPlaylistArtUrl(it.id)) }.convertToPagingLiveData()

    fun getPlaylistsWithPresence(
        filter: Filter<*>
    ): LiveData<List<PlaylistWithPresence>> =
        libraryDatabase
            .getPlaylistDao()
            .rawQueryListPlaylistsWithPresence(
                queryComposer.getQueryForPlaylistWithPresence(filter)
            )
            .map { list -> list.map { it.toViewPlaylist(urlRepository.getPlaylistArtUrl(it.id)) } }

    fun <T : Any> getPlaylistSongs(
        playlistId: Long,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        libraryDatabase.getPlaylistSongsDao().songsFromPlaylist(playlistId).map { mapping(it) }
            .convertToPagingLiveData()

    suspend fun <T : Any> getPlaylistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbPlaylist) -> T
    ): List<T> =
        libraryDatabase.getPlaylistDao().rawQueryListDisplay(
            queryComposer.getQueryForPlaylistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun getSongCountForFilter(filter: Filter<*>) = libraryDatabase
        .getSongDao()
        .countForFilters(
            queryComposer.getQueryForSongCount(filter)
        )

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheEditSource.createPlaylist(name)
        syncRepository.playlists()
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditSource.deletePlaylist(id)
        libraryDatabase.getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun addSongsToPlaylist(filter: Filter<*>, playlistId: Long) {
        val newSongsList = libraryDatabase
            .getSongDao()
            .forCurrentFilters(queryComposer.getQueryForSongs(listOf(filter), emptyList()))
        val total = libraryDatabase.getPlaylistDao().getPlaylistCount(playlistId)
        ampacheEditSource.addToPlaylist(playlistId, newSongsList, total)
        libraryDatabase.getPlaylistSongsDao().upsert(
            newSongsList.mapIndexed { index, song ->
                DbPlaylistSongs(total + index, song, playlistId)
            }
        )
    }

    suspend fun removeSongsFromPlaylist(filter: Filter<*>, playlistId: Long) {
        val songList = libraryDatabase
            .getSongDao()
            .forCurrentFilters(queryComposer.getQueryForSongs(listOf(filter), emptyList()))
        removeSongsFromPlaylist(playlistId, songList)
    }

    suspend fun removeSongsFromPlaylist(
        playlistId: Long,
        songList: List<Long>
    ) {
        songList.forEach {
            ampacheEditSource.removeSongFromPlaylist(playlistId, it)
            libraryDatabase.getPlaylistSongsDao().deleteSongFromPlaylist(playlistId, it)
        }
    }
}