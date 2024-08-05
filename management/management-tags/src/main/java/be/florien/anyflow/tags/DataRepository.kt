package be.florien.anyflow.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbAlbumDisplay
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbSongDisplay
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplay
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterTagsCount
import javax.inject.Inject

@ServerScope
class DataRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    private val queryComposer = QueryComposer()

    /**
     * Paging
     */

    fun getSongs(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, SongDisplay> =
        libraryDatabase.getSongDao().rawQueryPaging(
            queryComposer.getQueryForSongFiltered(filters?.toQueryFilters(), search)
        ).map(DbSongDisplay::toViewSongDisplay)

    fun getArtists(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, Artist> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForArtistFiltered(filters?.toQueryFilters(), search)
        ).map(DbArtist::toViewArtist)

    fun getAlbums(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, Album> =
        libraryDatabase.getAlbumDao().rawQueryDisplayPaging(
            queryComposer.getQueryForAlbumFiltered(filters?.toQueryFilters(), search)
        ).map(DbAlbumDisplay::toViewAlbum)

    fun getAlbumArtists(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, Artist> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForAlbumArtistFiltered(filters?.toQueryFilters(), search)
        ).map(DbArtist::toViewArtist)

    fun getGenres(
        filters: List<Filter<*>>?,
        search: String?
    ): DataSource.Factory<Int, Genre> =
        libraryDatabase.getGenreDao().rawQueryPaging(
            queryComposer.getQueryForGenreFiltered(filters?.toQueryFilters(), search)
        ).map(DbGenre::toViewGenre)

    /**
     * List
     */

    suspend fun getSongsSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<SongDisplay> =
        libraryDatabase.getSongDao().rawQueryListDisplay(
            queryComposer.getQueryForSongFiltered(filters?.toQueryFilters(), search)
        ).map(DbSongDisplay::toViewSongDisplay)

    suspend fun getArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<Artist> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForArtistFiltered(filters?.toQueryFilters(), search)
        ).map(DbArtist::toViewArtist)

    suspend fun getAlbumsSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<Album> =
        libraryDatabase.getAlbumDao().rawQueryDisplayList(
            queryComposer.getQueryForAlbumFiltered(filters?.toQueryFilters(), search)
        ).map(DbAlbumDisplay::toViewAlbum)

    suspend fun getAlbumArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<Artist> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForAlbumArtistFiltered(filters?.toQueryFilters(), search)
        ).map(DbArtist::toViewArtist)

    suspend fun getGenresSearchedList(
        filters: List<Filter<*>>?,
        search: String
    ): List<Genre> =
        libraryDatabase.getGenreDao().rawQueryList(
            queryComposer.getQueryForGenreFiltered(filters?.toQueryFilters(), search)
        ).map(DbGenre::toViewGenre)

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

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterTagsCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getFilterDao()
            .getCount(queryComposer.getQueryForTagsCount(filterList.toQueryFilters()))
            .toViewFilterCount()
    }

    suspend fun getSongDuration(id: Long): Int = libraryDatabase.getSongDao().getSongDuration(id)
}