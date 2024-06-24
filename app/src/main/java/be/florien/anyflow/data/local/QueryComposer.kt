package be.florien.anyflow.data.local

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.toDbFilter
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Ordering
import be.florien.anyflow.extension.iLog
import com.google.firebase.crashlytics.FirebaseCrashlytics

class QueryComposer {
    fun getQueryForSongs(
        filters: List<Filter<*>>,
        orderingList: List<Ordering>
    ): SimpleSQLiteQuery {

        fun constructOrderStatement(): String {
            val filteredOrderedList =
                orderingList.filter { it.orderingType != Ordering.OrderingType.PRECISE_POSITION }

            val isSorted =
                filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it.orderingType != Ordering.OrderingType.RANDOM }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                filteredOrderedList.forEachIndexed { index, order ->
                    orderStatement += when (order.orderingSubject) {
                        Ordering.Subject.ALL -> " song.id"
                        Ordering.Subject.ARTIST -> " artist.basename"
                        Ordering.Subject.ALBUM_ARTIST -> " albumArtist.basename"
                        Ordering.Subject.ALBUM -> " album.basename"
                        Ordering.Subject.ALBUM_ID -> " song.albumId"
                        Ordering.Subject.DISC -> " song.disk"
                        Ordering.Subject.YEAR -> " song.year"
                        Ordering.Subject.GENRE -> " song.genre"
                        Ordering.Subject.TRACK -> " song.track"
                        Ordering.Subject.TITLE -> " song.titleForSort"
                    }
                    orderStatement += when (order.orderingType) {
                        Ordering.OrderingType.ASCENDING -> " ASC"
                        Ordering.OrderingType.DESCENDING -> " DESC"
                        else -> ""
                    }
                    if (index < filteredOrderedList.size - 1 && orderStatement.last() != ',') {
                        orderStatement += ","
                    }
                }
            }

            if (orderingList.isEmpty() || (orderingList.size == 1 && orderingList[0].orderingType != Ordering.OrderingType.RANDOM) || orderingList.any { it.orderingSubject == Ordering.Subject.ALL }) {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("This is not the order you looking for (orderStatement: $orderStatement)"))
            }

            return orderStatement
        }

        val filterList = filters.filterNot { it.type == Filter.FilterType.PODCAST_EPISODE_IS }
        return SimpleSQLiteQuery(
            "SELECT DISTINCT song.id FROM song" +
                    constructJoinStatement(filterList, orderingList) +
                    constructWhereStatement(filterList, "") +
                    constructOrderStatement()
        )
    }

    fun getQueryForPodcastEpisodes(
        filters: List<Filter<*>>,
        orderingList: List<Ordering>//todo: add ordering handling
    ): SimpleSQLiteQuery {

        val podcastFilters = filters.filterIsInstance<Filter<Long>>()
            .filter { it.type == Filter.FilterType.PODCAST_EPISODE_IS }
        val whereStatement =
            " WHERE podcastEpisode.id IN (${podcastFilters.joinToString(separator = ", ") { it.argument.toString() }})"

        return SimpleSQLiteQuery("SELECT DISTINCT podcastEpisode.id FROM podcastEpisode $whereStatement")
    }

    fun getQueryForAlbumFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT " +
                    "DISTINCT album.id AS albumId, " +
                    "album.name AS albumName, " +
                    "album.artistId AS albumArtistId, " +
                    "album.year,album.diskcount, " +
                    "artist.name AS albumArtistName, " +
                    "artist.summary " +
                    "FROM album " +
                    "JOIN artist ON album.artistid = artist.id " +
                    "JOIN song ON song.albumId = album.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " album.name LIKE ?", search) +
                    " ORDER BY album.basename COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") }
        )

    fun getQueryForAlbumArtistFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT " +
                    "DISTINCT artist.id, " +
                    "artist.name, " +
                    "artist.prefix, " +
                    "artist.basename, " +
                    "artist.summary " +
                    "FROM artist " +
                    "JOIN album ON album.artistId = artist.id " +
                    "JOIN song ON song.albumId = album.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " artist.name LIKE ?", search) +
                    " ORDER BY artist.basename COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForArtistFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT " +
                    "DISTINCT artist.id, " +
                    "artist.name, " +
                    "artist.prefix, " +
                    "artist.basename, " +
                    "artist.summary " +
                    "FROM artist " +
                    "JOIN song ON song.artistId = artist.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " artist.name LIKE ?", search) +
                    " ORDER BY artist.basename COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForGenreFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT " +
                    "DISTINCT genre.id, " +
                    "genre.name " +
                    "FROM genre " +
                    "JOIN songgenre ON genre.id = songgenre.genreid " +
                    "JOIN song ON song.id = songgenre.songid " +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " genre.name LIKE ?", search) +
                    " ORDER BY genre.name COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForSongFiltered(filterList: List<Filter<*>>?, search: String?) =
        SimpleSQLiteQuery(
            "SELECT " +
                    "DISTINCT song.id AS id," +
                    "song.title AS title," +
                    "artist.name AS artistName," +
                    "album.name AS albumName," +
                    "album.id AS albumId," +
                    "song.time AS time " +
                    "FROM song " +
                    "JOIN artist ON song.artistId = artist.id " +
                    "JOIN album ON song.albumId = album.id" +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, " song.title LIKE ?", search) +
                    " ORDER BY song.titleForSort COLLATE UNICODE",
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })

    fun getQueryForPlaylistFiltered(
        filterList: List<Filter<*>>?,
        search: String?
    ): SimpleSQLiteQuery {
        val query = "SELECT " +
                "DISTINCT playlist.id, " +
                "playlist.name, " +
                "playlist.owner " +
                "FROM playlist " +
                "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
                constructJoinStatement(filterList, joinSong = "playlistsongs.songid") +
                constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
                " ORDER BY playlist.name COLLATE UNICODE"
        iLog("Query for playlist filtered:\n$query")
        return SimpleSQLiteQuery(
            query,
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })
    }

    fun getQueryForPlaylistWithCountFiltered(
        filterList: List<Filter<*>>?,
        search: String?
    ): SimpleSQLiteQuery {
        val query = "SELECT " +
                "DISTINCT playlist.id, " +
                "playlist.name, " +
                "playlist.owner, " +
                "(SELECT COUNT(songId) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount " +
                "FROM playlist " +
                "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
                constructJoinStatement(filterList, joinSong = "playlistsongs.songid") +
                constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
                " ORDER BY playlist.name COLLATE UNICODE"
        iLog("Query for playlist filtered:\n$query")
        return SimpleSQLiteQuery(
            query,
            search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })
    }

    fun getQueryForPlaylistWithPresence(filter: Filter<*>): SimpleSQLiteQuery {
        val filterList = listOf(filter)
        val selectForPresence = "SELECT " +
                "COUNT(*) " +
                "FROM playlistSongs " +
                "JOIN song ON PlaylistSongs.songId = song.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, "") +
                " AND playlistsongs.playlistId = playlist.id"
        val query = "SELECT " +
                "DISTINCT playlist.id, " +
                "playlist.name, " +
                "(SELECT COUNT(*) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount, " +
                "($selectForPresence) as presence " +
                "FROM playlist " +
                "ORDER BY playlist.name COLLATE UNICODE"
        iLog("Query for playlist with presence:\n$query")
        return SimpleSQLiteQuery(
            query
        )
    }

    fun getQueryForSongCount(filter: Filter<*>): SimpleSQLiteQuery {
        val filterList = listOf(filter)
        return SimpleSQLiteQuery(
            "SELECT " +
                    "COUNT(DISTINCT Song.id) " +
                    "FROM Song " +
                    constructJoinStatement(filterList) +
                    constructWhereStatement(filterList, "")
        )
    }

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

    fun getQueryForDownload(filterList: List<Filter<*>>?) = SimpleSQLiteQuery(
        "INSERT INTO Download (songId) SELECT Song.id FROM Song"
                + constructJoinStatement(filterList)
                + constructWhereStatement(filterList, "")
                + " AND Song.local IS NULL "
                + "AND Song.id NOT IN (SELECT Download.songId FROM Download)"
    )

    fun getQueryForDownloadProgress(filterList: List<Filter<*>>?) = SimpleSQLiteQuery(
        "SELECT " +
                "COUNT(DISTINCT Song.id) as total, " +
                "COUNT(DISTINCT download.songId) as queued, " +
                "COUNT(DISTINCT song.local) AS downloaded " +
                "FROM song " +
                "LEFT JOIN download ON song.id = download.songId"
                + constructJoinStatement(filterList)
                + constructWhereStatement(filterList, "")
    )

    private fun constructJoinStatement(
        filterList: List<Filter<*>>?,
        orderingList: List<Ordering> = emptyList(),
        joinSong: String? = null
    ): String {
        if (filterList == null || filterList.isEmpty() && orderingList.isEmpty()) {
            return " "
        }
        val isJoiningArtist = orderingList.any { it.orderingSubject == Ordering.Subject.ARTIST }
        val albumJoinCount =
            filterList.countFilter { it.type == Filter.FilterType.ALBUM_ARTIST_IS }
        val isJoiningAlbum = orderingList.any { it.orderingSubject == Ordering.Subject.ALBUM }
        val isJoiningAlbumArtist =
            orderingList.any { it.orderingSubject == Ordering.Subject.ALBUM_ARTIST }
        val isJoiningSongGenre = orderingList.any { it.orderingSubject == Ordering.Subject.GENRE }
        val isJoiningGenreForOrdering =
            orderingList.any { it.orderingSubject == Ordering.Subject.GENRE }
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
        if (isJoiningGenreForOrdering) {
            join += " JOIN genre ON songgenre.genreId = genre.id"
        }
        if (joinSong != null && filterList.isNotEmpty()) {
            join += " LEFT JOIN song ON song.id = $joinSong"
        }
        for (i in 0 until albumJoinCount) {
            join += " JOIN album AS album$i ON album$i.id = song.albumid"
        }
        for (i in 0 until songGenreJoinCount) {
            join += " JOIN songgenre AS songgenre$i ON songgenre$i.songId = song.id"
        }
        for (i in 0 until playlistSongsJointCount) {
            join += " LEFT JOIN playlistsongs AS playlistsongs$i ON playlistsongs$i.songId = song.id"
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
        return if (!filterList.isNullOrEmpty() || !search.isNullOrBlank()) {
            var where = " WHERE"
            if (!search.isNullOrBlank()) {
                where += searchCondition
            }
            if (!filterList.isNullOrEmpty()) {
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