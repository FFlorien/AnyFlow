package be.florien.anyflow.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.data.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.logging.iLog

class QueryComposer {
    //todo optimize queries
    fun getQueryForSongs(
        filters: List<QueryFilter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery {

        fun constructOrderStatement(): String {
            val filteredOrderedList =
                orderingList.filter { it !is QueryOrdering.Precise }

            val isSorted =
                filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it !is QueryOrdering.Random }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                filteredOrderedList.forEachIndexed { index, order ->
                    orderStatement += order.subject.clause
                    orderStatement += when (order) {
                        is QueryOrdering.Ordered -> " ASC"
                        else -> ""
                    }
                    if (index < filteredOrderedList.size - 1 && orderStatement.last() != ',') {
                        orderStatement += ","
                    }
                }
            }

            return orderStatement
        }

        val filterList = filters.filterNot { it.type == QueryFilter.FilterType.PODCAST_EPISODE_IS }
        return ("SELECT DISTINCT song.id FROM song" +
                constructJoinStatement(filterList, orderingList) +
                constructWhereStatement(filterList, "") +
                constructOrderStatement()
                )
            .toSQLiteQuery()

    }

    fun getQueryForPodcastEpisodes(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery {

        val podcastFilters = filters
            .filter { it.type == QueryFilter.FilterType.PODCAST_EPISODE_IS }
        val whereStatement =
            " WHERE podcastEpisode.id IN (${podcastFilters.joinToString(separator = ", ") { it.argument }})"

        return ("SELECT DISTINCT podcastEpisode.id FROM podcastEpisode $whereStatement")
            .toSQLiteQuery()
    }

    fun getQueryForAlbumFiltered(filterList: List<QueryFilter>?, search: String?) =
        ("SELECT " +
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
                " ORDER BY album.basename COLLATE UNICODE")
            .toSQLiteQuery(search)

    fun getQueryForAlbumArtistFiltered(filterList: List<QueryFilter>?, search: String?) =
        ("SELECT " +
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
                " ORDER BY artist.basename COLLATE UNICODE")
            .toSQLiteQuery(search)

    fun getQueryForArtistFiltered(filterList: List<QueryFilter>?, search: String?) =
        ("SELECT " +
                "DISTINCT artist.id, " +
                "artist.name, " +
                "artist.prefix, " +
                "artist.basename, " +
                "artist.summary " +
                "FROM artist " +
                "JOIN song ON song.artistId = artist.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, " artist.name LIKE ?", search) +
                " ORDER BY artist.basename COLLATE UNICODE")
            .toSQLiteQuery(search)

    fun getQueryForGenreFiltered(filterList: List<QueryFilter>?, search: String?) =
        ("SELECT " +
                "DISTINCT genre.id, " +
                "genre.name " +
                "FROM genre " +
                "JOIN songgenre ON genre.id = songgenre.genreid " +
                "JOIN song ON song.id = songgenre.songid " +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, " genre.name LIKE ?", search) +
                " ORDER BY genre.name COLLATE UNICODE")
            .toSQLiteQuery(search)

    fun getQueryForSongFiltered(filterList: List<QueryFilter>?, search: String?) =
        ("SELECT " +
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
                " ORDER BY song.titleForSort COLLATE UNICODE")
            .toSQLiteQuery(search)

    fun getQueryForPlaylistFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner " +
            "FROM playlist " +
            "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filterList, joinSong = "playlistsongs.songid") +
            constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForPlaylistWithCountFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner, " +
            "(SELECT COUNT(songId) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount " +
            "FROM playlist " +
            "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filterList, joinSong = "playlistsongs.songid") +
            constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForPlaylistWithPresence(filter: QueryFilter): SimpleSQLiteQuery {
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
        return (query).toSQLiteQuery()
    }

    fun getQueryForSongCount(filter: QueryFilter): SimpleSQLiteQuery {
        val filterList = listOf(filter)
        return ("SELECT " +
                "COUNT(DISTINCT Song.id) " +
                "FROM Song " +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, ""))
            .toSQLiteQuery()

    }

    fun getQueryForCount(filterList: List<QueryFilter>) = ("SELECT " +
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
            constructWhereStatement(filterList, ""))
        .toSQLiteQuery()

    fun getQueryForDownload(filterList: List<QueryFilter>?) =
        ("INSERT INTO Download (songId, mediaType) SELECT Song.id, $SONG_MEDIA_TYPE FROM Song"
                + constructJoinStatement(filterList)
                + constructWhereStatement(filterList, "")
                + " AND Song.local IS NULL "
                + "AND Song.id NOT IN (SELECT Download.songId FROM Download)")
            .toSQLiteQuery()

    fun getQueryForDownloadProgress(filterList: List<QueryFilter>?) = ("SELECT " +
            "COUNT(DISTINCT Song.id) as total, " +
            "COUNT(DISTINCT download.songId) as queued, " +
            "COUNT(DISTINCT song.local) AS downloaded " +
            "FROM song " +
            "LEFT JOIN download ON song.id = download.songId"
            + constructJoinStatement(filterList)
            + constructWhereStatement(filterList, ""))
        .toSQLiteQuery()

    private fun String.toSQLiteQuery(search: String? = null): SimpleSQLiteQuery {
        iLog("Query:\n$this")
        search?.let {
            iLog("Search: $it")
        }
        return SimpleSQLiteQuery(this, search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })
    }

    private fun constructJoinStatement(
        filterList: List<QueryFilter>?,
        orderingList: List<QueryOrdering> = emptyList(),
        joinSong: String? = null
    ): String {
        if (filterList == null || filterList.isEmpty() && orderingList.isEmpty()) {
            return " "
        }
        val isJoiningArtist = orderingList.any { it.subject == QueryOrdering.Subject.ARTIST }
        val albumJoinCount =
            filterList.countFilter { it.type == QueryFilter.FilterType.ALBUM_ARTIST_IS }
        val isJoiningAlbum = orderingList.any { it.subject == QueryOrdering.Subject.ALBUM }
        val isJoiningAlbumArtist =
            orderingList.any { it.subject == QueryOrdering.Subject.ALBUM_ARTIST }
        val isJoiningSongGenre = orderingList.any { it.subject == QueryOrdering.Subject.GENRE }
        val isJoiningGenreForOrdering =
            orderingList.any { it.subject == QueryOrdering.Subject.GENRE }
        val songGenreJoinCount =
            filterList.countFilter { it.type == QueryFilter.FilterType.GENRE_IS }
        val playlistSongsJointCount =
            filterList.countFilter { it.type == QueryFilter.FilterType.PLAYLIST_IS }

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

    private fun List<QueryFilter>.countFilter(predicate: (QueryFilter) -> Boolean): Int {
        val baseCount = count(predicate)
        var childrenCount = 0
        forEach {
            childrenCount += it.children.countFilter(predicate)
        }
        return baseCount + childrenCount
    }

    private fun constructWhereStatement(
        filterList: List<QueryFilter>?,
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
        filterList: List<QueryFilter>,
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
                if (filter.children.isNotEmpty()) {
                    whereStatement += " ("
                }
                whereStatement += " ${filter.type.clause} ${filter.argument}".run {
                    when (filter.type) {
                        QueryFilter.FilterType.ALBUM_ARTIST_IS -> {
                            futureAlbumArtistLevel = albumArtistLevel + 1
                            this.replace(
                                QueryFilter.TABLE_COUNT_FORMAT,
                                albumArtistLevel.toString()
                            )
                        }

                        QueryFilter.FilterType.GENRE_IS -> {
                            futureGenreLevel = genreLevel + 1
                            this.replace(QueryFilter.TABLE_COUNT_FORMAT, genreLevel.toString())
                        }

                        QueryFilter.FilterType.PLAYLIST_IS -> {
                            futurePlaylistLevel = playlistLevel + 1
                            this.replace(QueryFilter.TABLE_COUNT_FORMAT, playlistLevel.toString())
                        }

                        else -> this
                    }
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