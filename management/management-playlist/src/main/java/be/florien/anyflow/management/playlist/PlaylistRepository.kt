package be.florien.anyflow.management.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import androidx.paging.PagingData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.data.server.datasource.playlist.AmpachePlaylistSource
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.toQueryFilter
import be.florien.anyflow.management.filters.toQueryFilters
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.management.playlist.model.PlaylistSong
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.management.playlist.model.PlaylistWithPresence
import be.florien.anyflow.management.playlist.work.PlaylistModificationWorker
import be.florien.anyflow.urls.UrlRepository
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbPlaylistSongs
import be.florien.anyflow.tags.local.query.QueryComposer
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ServerScope
class PlaylistRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampachePlaylistSource: AmpachePlaylistSource,
    private val urlRepository: UrlRepository,
    private val workManager: WorkManager
) {

    private val queryComposer = QueryComposer()

    //region Get Playlists
    fun getPlaylists(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, Playlist> =
        libraryDatabase.getPlaylistDao().rawQueryPaging(
            queryComposer.getQueryForPlaylist(filters?.toQueryFilters(), search)
        ).map {
            it.toViewPlaylist(urlRepository)
        }

    fun getAllPlaylistsWithCount(): LiveData<PagingData<PlaylistWithCount>> =
        libraryDatabase.getPlaylistDao().rawQueryWithCountPaging(
            queryComposer.getQueryForPlaylistWithCount(null, null)
        ).map { it.toViewPlaylist(urlRepository) }.convertToPagingLiveData()

    fun getPlaylistsWithPresence(
        filter: Filter<*>
    ): LiveData<List<PlaylistWithPresence>> =
        libraryDatabase
            .getPlaylistDao()
            .rawQueryPlaylistsWithPresenceUpdatable(
                queryComposer.getQueryForPlaylistWithPresence(filter.toQueryFilter())
            )
            .map { list -> list.map { it.toViewPlaylist(urlRepository) } }

    fun getPlaylistSongs(
        playlistId: Long
    ): LiveData<PagingData<PlaylistSong>> =
        libraryDatabase.getPlaylistSongsDao()
            .songsFromPlaylistPaging(playlistId)
            .map { it.toViewPlaylistSong() }
            .convertToPagingLiveData()

    suspend fun getPlaylistFiltered(
        filters: List<Filter<*>>?,
        search: String
    ): List<PlaylistWithCount> =
        libraryDatabase.getPlaylistDao().rawQueryWithCountList(
            queryComposer.getQueryForPlaylist(filters?.toQueryFilters(), search)
        ).map { it.toViewPlaylist(urlRepository) }

    suspend fun getSongCountForFilter(filter: Filter<*>) = libraryDatabase
        .getSongDao()
        .rawQueryForCountFiltered(
            queryComposer.getQueryForSongCount(filter.toQueryFilter())
        )
    //endregion

    //region Playlists modification
    suspend fun createPlaylist(name: String) {
        ampachePlaylistSource.createPlaylist(name)
//        syncRepository.playlists()todo
    }

    suspend fun deletePlaylist(id: Long) {
        ampachePlaylistSource.deletePlaylist(id)
        libraryDatabase.getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun addSongsToPlaylist(filter: Filter<*>, playlistId: Long) {
        val newSongsList = libraryDatabase
            .getSongDao()
            .forCurrentFiltersList(
                queryComposer.getQueryForSongIds(
                    listOf(filter).toQueryFilters(),
                    emptyList()
                )
            )
        val total = libraryDatabase.getPlaylistDao().getPlaylistCount(playlistId)
        val inputData = PlaylistModificationWorker.getDataForAdding(playlistId, newSongsList, total)
        val workRequest = OneTimeWorkRequestBuilder<PlaylistModificationWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.SECONDS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(inputData)
            .build()
        workManager.enqueue(workRequest)
        libraryDatabase.getPlaylistSongsDao().upsert(
            newSongsList.mapIndexed { index, song ->
                DbPlaylistSongs(total + index, song, playlistId)
            }
        )
    }

    suspend fun removeSongsFromPlaylist(filter: Filter<*>, playlistId: Long) {
        val songList = libraryDatabase
            .getSongDao()
            .forCurrentFiltersList(
                queryComposer.getQueryForSongIds(
                    listOf(filter).toQueryFilters(),
                    emptyList()
                )
            )
        removeSongsFromPlaylist(playlistId, songList)
    }

    suspend fun removeSongsFromPlaylist(
        playlistId: Long,
        songList: List<Long>
    ) {
        val inputData = PlaylistModificationWorker.getDataForRemoving(playlistId, songList)
        val workRequest = OneTimeWorkRequestBuilder<PlaylistModificationWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueue(workRequest)
        songList.forEach {
            libraryDatabase.getPlaylistSongsDao().deleteSongFromPlaylist(playlistId, it)
        }
    }
    //endregion
}