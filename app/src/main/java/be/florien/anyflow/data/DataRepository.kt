package be.florien.anyflow.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.query.QueryComposer
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterCount
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.convertToPagingLiveData
import be.florien.anyflow.injection.ServerScope
import javax.inject.Inject

@ServerScope
class DataRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    private val queryComposer = QueryComposer()

    /**
     * Paging
     */

    fun <T : Any> getSongs(
        mapping: (DbSongDisplay) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getSongDao().rawQueryPaging(
            queryComposer.getQueryForSongFiltered(filters?.toQueryFilters(), search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForArtistFiltered(filters?.toQueryFilters(), search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getAlbums(
        mapping: (DbAlbumDisplay) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getAlbumDao().rawQueryDisplayPaging(
            queryComposer.getQueryForAlbumFiltered(filters?.toQueryFilters(), search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getAlbumArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForAlbumArtistFiltered(filters?.toQueryFilters(), search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getGenres(
        mapping: (DbGenre) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getGenreDao().rawQueryPaging(
            queryComposer.getQueryForGenreFiltered(filters?.toQueryFilters(), search)
        ).map { mapping(it) }.convertToPagingLiveData()

    /**
     * List
     */

    suspend fun <T : Any> getSongsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbSongDisplay) -> T
    ): List<T> =
        libraryDatabase.getSongDao().rawQueryListDisplay(
            queryComposer.getQueryForSongFiltered(filters?.toQueryFilters(), search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForArtistFiltered(filters?.toQueryFilters(), search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getAlbumsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbAlbumDisplay) -> T,
    ): List<T> =
        libraryDatabase.getAlbumDao().rawQueryDisplayList(
            queryComposer.getQueryForAlbumFiltered(filters?.toQueryFilters(), search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getAlbumArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForAlbumArtistFiltered(filters?.toQueryFilters(), search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getGenresSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbGenre) -> T
    ): List<T> =
        libraryDatabase.getGenreDao().rawQueryList(
            queryComposer.getQueryForGenreFiltered(filters?.toQueryFilters(), search)
        ).map { item -> (mapping(item)) }

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

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getFilterDao().getCount(queryComposer.getQueryForCount(filterList.toQueryFilters()))
            .toViewFilterCount()
    }

    suspend fun getSongDuration(id: Long): Int = libraryDatabase.getSongDao().getSongDuration(id)
}