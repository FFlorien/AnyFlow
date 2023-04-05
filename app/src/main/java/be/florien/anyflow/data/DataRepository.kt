package be.florien.anyflow.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.DbAlbumDisplayForRaw
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.local.model.DbGenre
import be.florien.anyflow.data.local.model.DbSongDisplay
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
            queryComposer.getQueryForSongFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForArtistFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getAlbums(
        mapping: (DbAlbumDisplayForRaw) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getAlbumDao().rawQueryPaging(
            queryComposer.getQueryForAlbumFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getAlbumArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getArtistDao().rawQueryPaging(
            queryComposer.getQueryForAlbumArtistFiltered(filters, search)
        ).map { mapping(it) }.convertToPagingLiveData()

    fun <T : Any> getGenres(
        mapping: (DbGenre) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        libraryDatabase.getGenreDao().rawQueryPaging(
            queryComposer.getQueryForGenreFiltered(filters, search)
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
            queryComposer.getQueryForSongFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForArtistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getAlbumsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbAlbumDisplayForRaw) -> T,
    ): List<T> =
        libraryDatabase.getAlbumDao().rawQueryListDisplay(
            queryComposer.getQueryForAlbumFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getAlbumArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQueryList(
            queryComposer.getQueryForAlbumArtistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getGenresSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbGenre) -> T
    ): List<T> =
        libraryDatabase.getGenreDao().rawQueryList(
            queryComposer.getQueryForGenreFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    /**
     * Songs related methods
     */

    fun searchSongs(filter: String) =
        libraryDatabase.getSongDao().searchPositionsWhereFilterPresent("%$filter%")

    fun getSong(id: Long): LiveData<SongInfo> =
        libraryDatabase.getSongDao().findById(id).map { it.toViewSongInfo() }

    /**
     * Infos
     */

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getFilterDao().getCount(queryComposer.getQueryForCount(filterList))
            .toViewFilterCount()
    }
}