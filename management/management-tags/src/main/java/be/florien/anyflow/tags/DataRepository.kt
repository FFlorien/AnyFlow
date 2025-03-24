package be.florien.anyflow.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterTagsCount
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbAlbumDisplay
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbDownloadedCount
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbSongDisplay
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.DownloadedCount
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplayDomain
import be.florien.anyflow.tags.model.SongInfo
import javax.inject.Inject

@ServerScope
class DataRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val queryComposer: QueryComposer
) {
    /**
     * Paging
     */

    fun getSongs(
        filter: Filter?,
        search: String?
    ): DataSource.Factory<Int, SongDisplayDomain> =
        libraryDatabase.getSongDao().rawQueryPaging(
            queryComposer.getQueryForSong(filter, search)
        ).map(DbSongDisplay::toDomainSongDisplay)

    fun getArtists(
        filter: Filter?,
        search: String?
    ): DataSource.Factory<Int, Artist> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForArtist(filter, search)
        ).map(DbArtist::toViewArtist)

    fun getAlbums(
        filter: Filter?,
        search: String?
    ): DataSource.Factory<Int, Album> =
        libraryDatabase.getAlbumDao().rawQueryDisplayPaging(
            queryComposer.getQueryForAlbum(filter, search)
        ).map(DbAlbumDisplay::toViewAlbum)

    fun getAlbumArtists(
        filter: Filter?,
        search: String?
    ): DataSource.Factory<Int, Artist> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForAlbumArtist(filter, search)
        ).map(DbArtist::toViewArtist)

    fun getGenres(
        filter: Filter?,
        search: String?
    ): DataSource.Factory<Int, Genre> =
        libraryDatabase.getGenreDao().rawQueryPaging(
            queryComposer.getQueryForGenre(filter, search)
        ).map(DbGenre::toViewGenre)

    fun getDownloadedInfo(
        filter: Filter?
    ): DataSource.Factory<Int, DownloadedCount> =
        libraryDatabase.getDownloadDao().rawQueryPaging(
            queryComposer.getQueryForDownloadCount(filter)
        ).map(DbDownloadedCount::toViewDownloadedCount)

    /**
     * List
     */

    suspend fun getSongFiltered(
        filter: Filter?,
        search: String
    ): List<SongDisplayDomain> =
        libraryDatabase.getSongDao().rawQueryListDisplay(
            queryComposer.getQueryForSong(filter, search)
        ).map(DbSongDisplay::toDomainSongDisplay)

    suspend fun getArtistFiltered(
        filter: Filter?,
        search: String
    ): List<Artist> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForArtist(filter, search)
        ).map(DbArtist::toViewArtist)

    suspend fun getAlbumFiltered(
        filter: Filter?,
        search: String
    ): List<Album> =
        libraryDatabase.getAlbumDao().rawQueryDisplayList(
            queryComposer.getQueryForAlbum(filter, search)
        ).map(DbAlbumDisplay::toViewAlbum)

    suspend fun getAlbumArtistFiltered(
        filter: Filter?,
        search: String
    ): List<Artist> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForAlbumArtist(filter, search)
        ).map(DbArtist::toViewArtist)

    suspend fun getGenreFiltered(
        filter: Filter?,
        search: String
    ): List<Genre> =
        libraryDatabase.getGenreDao().rawQueryList(
            queryComposer.getQueryForGenre(filter, search)
        ).map(DbGenre::toViewGenre)

    suspend fun getDownloadedSearchedList(
        filter: Filter?
    ): List<DownloadedCount> =
        libraryDatabase.getDownloadDao().rawQueryCountList(
            queryComposer.getQueryForDownloadCount(filter)
        ).map(DbDownloadedCount::toViewDownloadedCount)

    /**
     * Songs related methods
     */

    fun searchSongs(filter: String) =
        libraryDatabase.getSongDao().searchPositionsWhereFilterPresentUpdatable("%$filter%")

    fun getSong(id: Long): LiveData<SongInfo> =
        libraryDatabase.getSongDao().songByIdUpdatable(id).map { it.toViewSongInfo() }

    suspend fun getSongSync(id: Long): SongInfo =
        libraryDatabase.getSongDao().songById(id).toViewSongInfo()

    /**
     * Infos
     */

    suspend fun getFilteredInfo(infoSource: Filter?): FilterTagsCount {
        return libraryDatabase.getFilterDao()
            .getCount(queryComposer.getQueryForTagsCount(infoSource))
            .toViewFilterCount()
    }

    suspend fun getSongDuration(id: Long): Int = libraryDatabase.getSongDao().getSongDuration(id)
}