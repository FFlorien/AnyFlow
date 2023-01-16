package be.florien.anyflow.data.local

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.toDbFilter
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Order
import com.google.firebase.crashlytics.FirebaseCrashlytics

class QueryComposer {
    fun getQueryForSongs(
        filters: List<Filter<*>>,
        orderList: List<Order>
    ): SimpleSQLiteQuery {

        fun constructOrderStatement(): String {
            val filteredOrderedList =
                orderList.filter { it.orderingType != Order.Ordering.PRECISE_POSITION }

            val isSorted =
                filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it.orderingType != Order.Ordering.RANDOM }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                filteredOrderedList.forEachIndexed { index, order ->
                    orderStatement += when (order.orderingSubject) {
                        Order.Subject.ALL -> " song.id"
                        Order.Subject.ARTIST -> " artist.name"
                        Order.Subject.ALBUM_ARTIST -> " albumArtist.name"
                        Order.Subject.ALBUM -> " album.name"
                        Order.Subject.ALBUM_ID -> " song.albumId"
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

            if (orderList.isEmpty() || (orderList.size == 1 && orderList[0].orderingType != Order.Ordering.RANDOM) || orderList.any { it.orderingSubject == Order.Subject.ALL }) {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("This is not the order you looking for (orderStatement: $orderStatement)"))
            }

            return orderStatement
        }

        return SimpleSQLiteQuery(
            "SELECT DISTINCT song.id FROM song" +
                    constructJoinStatement(filters, orderList) +
                    constructWhereStatement(filters, "") +
                    constructOrderStatement()
        )
    }

    fun getQueryForAlbumFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT album.id AS albumId, album.name AS albumName, album.artistId AS albumArtistId, album.year,album.diskcount, artist.name AS albumArtistName, artist.summary FROM album JOIN artist ON album.artistid = artist.id JOIN song ON song.albumId = album.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " album.name LIKE ?", search) +
                    " ORDER BY album.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") }
        )

    fun getQueryForAlbumArtistFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist JOIN album ON album.artistId = artist.id JOIN song ON song.albumId = album.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " artist.name LIKE ?", search) +
                    " ORDER BY artist.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForArtistFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist JOIN song ON song.artistId = artist.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " artist.name LIKE ?", search) +
                    " ORDER BY artist.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })


    fun getQueryForGenreFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT genre.id, genre.name FROM genre JOIN songgenre ON genre.id = songgenre.genreid JOIN song ON song.id = songgenre.songid " +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " genre.name LIKE ?", search) +
                    " ORDER BY genre.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForSongFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT song.id, song.title, song.artistId, song.albumId, song.track, song.disk, song.time, song.year, song.composer, song.local, song.bars FROM song" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " song.title LIKE ?", search) +
                    " ORDER BY song.title COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForPlaylistFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT DISTINCT playlist.id, playlist.name, (SELECT COUNT(*) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount FROM playlist LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id LEFT JOIN song ON playlistsongs.songId = song.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
                    " ORDER BY playlist.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForCount(filterList: List<Filter<*>>) = SimpleSQLiteQuery(
        "SELECT " +
                "SUM(Song.time) AS duration, " +
                "COUNT(DISTINCT SongGenre.genreId) AS genres, " +
                "COUNT(DISTINCT Album.artistid) AS albumArtists, " +
                "COUNT(DISTINCT Song.albumId) AS albums, " +
                "COUNT(DISTINCT Song.artistId) AS artists, " +
                "COUNT(DISTINCT Song.id) AS songs, " +
                "COUNT(DISTINCT PlaylistSongs.playlistId) AS playlists " +
                "FROM Song " +
                "LEFT JOIN SongGenre ON Song.id = SongGenre.songId " +
                "JOIN Album ON Song.albumId = Album.id " +
                "LEFT JOIN PlaylistSongs ON Song.id = PlaylistSongs.songId" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, "")
    )


    private fun constructJoinStatement(
        filterList: List<Filter<*>>?,
        orderList: List<Order> = emptyList()
    ): String {
        if (filterList == null || filterList.isEmpty() && orderList.isEmpty()) {
            return " "
        }
        val isJoiningArtist = orderList.any { it.orderingSubject == Order.Subject.ARTIST }
        val albumJoinCount =
            filterList.countFilter { it.type == Filter.FilterType.ALBUM_ARTIST_IS }
        val isJoiningAlbum = orderList.any { it.orderingSubject == Order.Subject.ALBUM }
        val isJoiningAlbumArtist =
            orderList.any { it.orderingSubject == Order.Subject.ALBUM_ARTIST }
        val isJoiningSongGenre = orderList.any { it.orderingSubject == Order.Subject.GENRE }
        val isJoiningGenreForOrder = orderList.any { it.orderingSubject == Order.Subject.GENRE }
        val songGenreJoinCount = filterList.countFilter { it.type == Filter.FilterType.GENRE_IS }
        val playlistSongsJointCount =
            filterList.countFilter { it.type == Filter.FilterType.PLAYLIST_IS }

        var join = ""
        if (isJoiningArtist) {
            join += " JOIN artist ON song.artistId = artist.id"
        }
        if (isJoiningAlbum || isJoiningAlbumArtist) {
            join += " JOIN album ON song.albumId = album.id"
        }
        if (isJoiningAlbumArtist) {
            join += " JOIN artist AS albumArtist ON album.artistId = albumArtist.id"
        }
        if (isJoiningSongGenre) {
            join += " JOIN songgenre ON songgenre.songId = song.id"
        }
        if (isJoiningGenreForOrder) {
            join += " JOIN genre ON songgenre.genreId = genre.id"
        }
        for (i in 0 until albumJoinCount) {
            join += " JOIN album AS album$i ON album$i.id = song.albumid"
        }
        for (i in 0 until songGenreJoinCount) {
            join += " JOIN songgenre AS songgenre$i ON songgenre$i.songId = song.id"
        }
        for (i in 0 until playlistSongsJointCount) {
            join += " JOIN playlistsongs AS playlistsongs$i ON playlistsongs$i.songId = song.id"
        }

        return join
    }

    private fun List<Filter<*>>.countFilter(predicate: (Filter<*>) -> Boolean): Int {
        val baseCount = count(predicate)
        var childrenCount = 0
        forEach {
            childrenCount += it.children.countFilter(predicate)
        }
        return baseCount + childrenCount
    }

    private fun constructWhereStatement(
        filterList: List<Filter<*>>?,
        searchCondition: String,
        search: String? = null
    ): String {
        return if ((filterList != null && filterList.isNotEmpty()) || search != null && search.isNotBlank()) {
            var where = " WHERE"
            if (search != null && search.isNotBlank()) {
                where += searchCondition
            }
            if (filterList != null && filterList.isNotEmpty()) {
                where += constructWhereSubStatement(filterList, 0, 0, 0)
            }
            where
        } else {
            ""
        }
    }

    private fun constructWhereSubStatement(
        filterList: List<Filter<*>>,
        genreLevel: Int,
        playlistLevel: Int,
        albumArtistLevel: Int
    ): String {
        var futureGenreLevel = genreLevel
        var futurePlaylistLevel = playlistLevel
        var futureAlbumArtistLevel = albumArtistLevel
        var whereStatement = ""
        filterList
            .forEachIndexed { index, filter ->
                val dbFilter = filter.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID)
                if (filter.children.isNotEmpty()) {
                    whereStatement += " ("
                }
                whereStatement += when (dbFilter.clause) {
                    DbFilter.SONG_ID,
                    DbFilter.ARTIST_ID,
                    DbFilter.ALBUM_ID -> " ${dbFilter.clause} ${dbFilter.argument.toLong()}"
                    DbFilter.ALBUM_ARTIST_ID -> {
                        futureAlbumArtistLevel = albumArtistLevel + 1
                        " album${albumArtistLevel}.artistid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.GENRE_IS -> {
                        futureGenreLevel = genreLevel + 1
                        " songgenre${genreLevel}.genreid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.PLAYLIST_ID -> {
                        futurePlaylistLevel = playlistLevel + 1
                        " playlistsongs${playlistLevel}.playlistid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.DOWNLOADED -> " ${DbFilter.DOWNLOADED}"
                    DbFilter.NOT_DOWNLOADED -> " ${DbFilter.NOT_DOWNLOADED}"
                    else -> " ${dbFilter.clause} \"${dbFilter.argument}\""
                }
                if (filter.children.isNotEmpty()) {
                    whereStatement += " AND" + constructWhereSubStatement(
                        filter.children,
                        futureGenreLevel,
                        futurePlaylistLevel,
                        futureAlbumArtistLevel
                    )
                    whereStatement += ")"
                }
                if (index < filterList.size - 1) {
                    whereStatement += " OR"
                }
            }
        return whereStatement
    }
}