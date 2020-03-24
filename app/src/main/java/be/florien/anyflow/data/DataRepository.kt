package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.local.model.DbArtistDisplay
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.server.model.AmpacheAlbumList
import be.florien.anyflow.data.server.model.AmpacheArtistList
import be.florien.anyflow.data.server.model.AmpacheError
import be.florien.anyflow.data.server.model.AmpacheSongList
import be.florien.anyflow.data.view.*
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.getDate
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Update the local data with the one from the server
 */
@Singleton
class DataRepository
@Inject constructor(
        private val libraryDatabase: LibraryDatabase,
        private val songServerConnection: AmpacheConnection,
        private val sharedPreferences: SharedPreferences) {

    val changeUpdater
        get() = libraryDatabase.changeUpdater
    var randomOrderingSeed = 2
    var precisePosition = listOf<Order>()

    private fun lastAcceptableUpdate() = Calendar.getInstance().apply {
        add(Calendar.HOUR, -1)
    }

    /**
     * Getter with server updates
     */

    fun updateAll(): Completable = updateArtists()
            .concatWith(updateAlbums())
            .concatWith(updateSongs())

    fun getSongAtPosition(position: Int) = libraryDatabase.getSongAtPosition(position).map { it.toViewSong() }

    fun getPositionForSong(song: Song) = libraryDatabase.getPositionForSong(song.toDbSongDisplay())

    fun getSongsInQueueOrder() = convertToFlowable(libraryDatabase.getSongsInQueueOrder().map { it.toViewSong() }, "songs")
    fun <T> getAlbums(mapping: (DbAlbumDisplay) -> T): Flowable<PagedList<T>> = convertToFlowable(libraryDatabase.getAlbums().map { mapping(it) }, "albums")
    fun <T> getArtists(mapping: (DbArtistDisplay) -> T): Flowable<PagedList<T>> = convertToFlowable(libraryDatabase.getArtists().map { mapping(it) }, "artists")
    fun <T> getGenres(mapping: (String) -> T): Flowable<PagedList<T>> = convertToFlowable(libraryDatabase.getGenres().map { mapping(it) }, "genres")

    fun getOrders() = libraryDatabase.getOrders().map { orderList -> orderList.map { it.toViewOrder() } }
    fun getOrderlessQueue(): Flowable<List<Long>> = Flowable.merge(getPlaylistFromFilter(), getPlaylistFromOrder())
    fun setOrders(orders: MutableList<Order>) = libraryDatabase.setOrders(orders.map { it.toDbOrder() })
    fun setOrdersSubject(orderSubjects: List<Long>): Completable {
        val dbOrders = orderSubjects.mapIndexed { index, order -> Order(index, order, Order.ASCENDING).toDbOrder() }
        return libraryDatabase.setOrders(dbOrders)
    }

    fun saveQueueOrder(listToSave: MutableList<Long>) {
        libraryDatabase.saveQueueOrder(listToSave)
    }

    fun createFilterGroup(filterList: List<Filter<*>>, name: String) = libraryDatabase.createFilterGroup(filterList.map { it.toDbFilter(-1) }, name)
    fun getFilterGroups() = libraryDatabase.getFilterGroups().map { groupList -> groupList.map { it.toViewFilterGroup() } }
    fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) = libraryDatabase.setSavedGroupAsCurrentFilters(filterGroup.toDbFilterGroup())
    fun getAlbumArtsForFilterGroup() = libraryDatabase.getFilterGroups()
            .map { groups -> groups.map { group -> libraryDatabase.filterForGroupSync(group.id) } }
            .map { filters -> filters.map { filterList -> libraryDatabase.artForFilters(constructWhereStatement(filterList.map { it.toViewFilter() })) } }
            .doOnError { this@DataRepository.eLog(it, "Error while querying filtersForGroup") }.subscribeOn(Schedulers.io())

    fun getCurrentFilters() = libraryDatabase.getCurrentFilters().map { filterList -> filterList.map { it.toViewFilter() } }
    fun setCurrentFilters(filterList: List<Filter<*>>) = libraryDatabase.setCurrentFilters(filterList.map { it.toDbFilter(1) })

    /**
     * Private Method
     */

    private fun updateSongs(): Completable = getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            AmpacheSongList::error
    ) { ampacheSongList ->
        val songs = ampacheSongList.songs.map { it.toDbSong() }
        libraryDatabase
                .addSongs(songs)
                .concatWith(libraryDatabase.correctAlbumArtist(songs))
    }

    private fun updateArtists(): Completable = getUpToDateList(
            LAST_ARTIST_UPDATE,
            AmpacheConnection::getArtists,
            AmpacheArtistList::error
    ) { ampacheArtistList ->
        libraryDatabase.addArtists(ampacheArtistList.artists.map { it.toDbArtist() })
    }

    private fun updateAlbums(): Completable = getUpToDateList(
            LAST_ALBUM_UPDATE,
            AmpacheConnection::getAlbums,
            AmpacheAlbumList::error
    ) { ampacheAlbumList ->
        libraryDatabase.addAlbums(ampacheAlbumList.albums.map { it.toDbAlbum() })
    }

    private fun <SERVER_TYPE> getUpToDateList(
            updatePreferenceName: String,
            getListOnServer: AmpacheConnection.(Calendar) -> Observable<SERVER_TYPE>,
            getError: SERVER_TYPE.() -> AmpacheError,
            saveToDatabase: (SERVER_TYPE) -> Completable)
            : Completable {
        val nowDate = Calendar.getInstance()
        val lastUpdate = sharedPreferences.getDate(updatePreferenceName, 0)
        val lastAcceptableUpdate = lastAcceptableUpdate()

        return if (lastUpdate.before(lastAcceptableUpdate)) {
            songServerConnection
                    .getListOnServer(lastUpdate)
                    .flatMapCompletable { result ->
                        saveToDatabase(result).doFinally {
                            when (result.getError().code) {
                                401 -> songServerConnection.reconnect(songServerConnection.getListOnServer(lastUpdate))
                                else -> Observable.just(result)
                            }
                        }
                    }.doOnComplete {
                        sharedPreferences.applyPutLong(updatePreferenceName, nowDate.timeInMillis)
                    }
        } else {
            Completable.complete()
        }
    }

    private fun <T> convertToFlowable(dataSourceFactory: DataSource.Factory<Int, T>, errorName: String): Flowable<PagedList<T>> {
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig)
                .buildFlowable(BackpressureStrategy.LATEST)
                .doOnError { this@DataRepository.eLog(it, "Error while querying $errorName") }
                .subscribeOn(Schedulers.io())
    }

    private fun getPlaylistFromFilter(): Flowable<List<Long>> =
            getCurrentFilters()
                    .withLatestFrom(
                            getOrders(),
                            BiFunction { dbFilters: List<Filter<*>>, dbOrders: List<Order> ->
                                getQueryForSongs(dbFilters, dbOrders)
                            })
                    .flatMap {
                        libraryDatabase.getSongsFromQuery(it)
                    }
                    .doOnError { this@DataRepository.eLog(it, "Error while querying getPlaylistFromFilter") }

    private fun getPlaylistFromOrder(): Flowable<List<Long>> =
            getOrders()
                    .doOnNext { retrieveRandomness(it) }
                    .withLatestFrom(
                            getCurrentFilters(),
                            BiFunction { dbOrders: List<Order>, dbFilters: List<Filter<*>> ->
                                getQueryForSongs(dbFilters, dbOrders)
                            })
                    .flatMap {
                        libraryDatabase.getSongsFromQuery(it)
                    }
                    .doOnError { this@DataRepository.eLog(it, "Error while querying getPlaylistFromOrder") }

    private fun retrieveRandomness(orderList: List<Order>) {
        randomOrderingSeed = orderList
                .firstOrNull { it.orderingType == Order.Ordering.RANDOM }
                ?.argument ?: -1
        precisePosition = orderList.filter { it.orderingType == Order.Ordering.PRECISE_POSITION }
    }

    private fun getQueryForSongs(dbFilters: List<Filter<*>>, orderList: List<Order>): String {

        fun constructOrderStatement(): String {
            val filteredOrderedList = orderList.filter { it.orderingType != Order.Ordering.PRECISE_POSITION }

            val isSorted = filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it.orderingType != Order.Ordering.RANDOM }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                filteredOrderedList.forEachIndexed { index, order ->
                    orderStatement += when (order.orderingSubject) {
                        Order.Subject.ALL -> " song.id"
                        Order.Subject.ARTIST -> " song.artistName"
                        Order.Subject.ALBUM_ARTIST -> " song.albumArtistName"
                        Order.Subject.ALBUM -> " song.albumName"
                        Order.Subject.YEAR -> " song.year"
                        Order.Subject.GENRE -> " song.genre"
                        Order.Subject.TRACK -> " song.track"
                        Order.Subject.TITLE -> " song.title"
                    }
                    orderStatement += when (order.orderingType) {
                        Order.Ordering.ASCENDING -> " ASC"
                        Order.Ordering.DESCENDING -> " DESC"
                        else -> ""
                    }
                    if (index < filteredOrderedList.size - 1 && orderStatement.last() != ',') {
                        orderStatement += ","
                    }
                }
            }

            return orderStatement
        }

        return "SELECT id FROM song" + constructWhereStatement(dbFilters) + constructOrderStatement()
    }

    private fun constructWhereStatement(filterList: List<Filter<*>>): String {
        var whereStatement = if (filterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        filterList.forEachIndexed { index, filter ->
            whereStatement += when (filter) {
                is Filter.TitleIs,
                is Filter.TitleContain,
                is Filter.Search,
                is Filter.GenreIs -> " ${filter.clause} \"${filter.argument}\""
                is Filter.SongIs,
                is Filter.ArtistIs,
                is Filter.AlbumArtistIs,
                is Filter.AlbumIs -> " ${filter.clause} ${filter.argument}"
            }
            if (index < filterList.size - 1) {
                whereStatement += " OR"
            }
        }

        return whereStatement
    }

    companion object {
        private const val LAST_SONG_UPDATE = "LAST_SONG_UPDATE"

        // updated because the art was added , see LibraryDatabase migration 1->2
        private const val LAST_ARTIST_UPDATE = "LAST_ARTIST_UPDATE_v1"
        private const val LAST_ALBUM_UPDATE = "LAST_ALBUM_UPDATE"
    }
}