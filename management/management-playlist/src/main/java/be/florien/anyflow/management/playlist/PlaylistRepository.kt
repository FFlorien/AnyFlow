package be.florien.anyflow.management.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import androidx.paging.PagingData
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbPlaylistSongs
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.data.server.datasource.playlist.AmpachePlaylistSource
import be.florien.anyflow.tags.toQueryFilter
import be.florien.anyflow.tags.toQueryFilters
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.management.playlist.model.PlaylistSong
import be.florien.anyflow.management.playlist.model.PlaylistWithPresence
import javax.inject.Inject

@ServerScope
class PlaylistRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheEditSource: AmpachePlaylistSource,
    private val urlRepository: UrlRepository
) {

    private val queryComposer = QueryComposer()

    fun getPlaylists(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int,Playlist> =
        libraryDatabase.getPlaylistDao().rawQueryWithCountPaging(
            queryComposer.getQueryForPlaylistWithCountFiltered(filters?.toQueryFilters(), search)
        ).map {
            it.toViewPlaylist(urlRepository)
        }

    fun getAllPlaylists(): LiveData<PagingData<Playlist>> =
        libraryDatabase.getPlaylistDao().rawQueryWithCountPaging(
            queryComposer.getQueryForPlaylistWithCountFiltered(null, null)
        ).map { it.toViewPlaylist(urlRepository) }.convertToPagingLiveData()

    fun getPlaylistsWithPresence(
        filter: Filter<*>
    ): LiveData<List<PlaylistWithPresence>> =
        libraryDatabase
            .getPlaylistDao()
            .rawQueryPlaylistsWithPresenceUpdatable(
                queryComposer.getQueryForPlaylistWithPresence(filter.toQueryFilter())
            )
            .map { list -> list.map { it.toViewPlaylist() } }

    fun getPlaylistSongs(
        playlistId: Long
    ): LiveData<PagingData<PlaylistSong>> =
        libraryDatabase.getPlaylistSongsDao()
            .songsFromPlaylistPaging(playlistId)
            .map { it.toViewPlaylistSong() }
            .convertToPagingLiveData()

    suspend fun getPlaylistsSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<Playlist> =
        libraryDatabase.getPlaylistDao().rawQueryWithCountList(
            queryComposer.getQueryForPlaylistFiltered(filters?.toQueryFilters(), search)
        ).map { it.toViewPlaylist(urlRepository) }

    suspend fun getSongCountForFilter(filter: Filter<*>) = libraryDatabase
        .getSongDao()
        .rawQueryForCountFiltered(
            queryComposer.getQueryForSongCount(filter.toQueryFilter())
        )

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheEditSource.createPlaylist(name)
//        syncRepository.playlists()todo
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditSource.deletePlaylist(id)
        libraryDatabase.getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun addSongsToPlaylist(filter: Filter<*>, playlistId: Long) {
        val newSongsList = libraryDatabase
            .getSongDao()
            .forCurrentFiltersList(queryComposer.getQueryForSongs(listOf(filter).toQueryFilters(), emptyList()))
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
            .forCurrentFiltersList(queryComposer.getQueryForSongs(listOf(filter).toQueryFilters(), emptyList()))
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