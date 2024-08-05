package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.logging.iLog

class QueryComposer {
    //todo optimize queries
    fun getQueryForSongs(
        filters: List<QueryFilter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery {
        val filterList = filters.filterNot { it.type == QueryFilter.FilterType.PODCAST_EPISODE_IS }
        return ("SELECT DISTINCT song.id FROM song" +
                constructJoinStatement(filterList, orderingList) +
                constructWhereStatement(filterList, "") +
                constructOrderStatement(orderingList))
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

    fun getQueryForAlbumFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
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

    fun getQueryForAlbumArtistFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
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

    fun getQueryForArtistFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
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

    fun getQueryForGenreFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT genre.id, " +
            "genre.name " +
            "FROM genre " +
            "JOIN songgenre ON genre.id = songgenre.genreid " +
            "JOIN song ON song.id = songgenre.songid " +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " genre.name LIKE ?", search) +
            " ORDER BY genre.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForSongFiltered(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
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
            constructJoinStatement(filterList, shouldJoinSong = true) +
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
            constructJoinStatement(filterList, shouldJoinSong = true) +
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
        return ("SELECT " +
                "DISTINCT playlist.id, " +
                "playlist.name, " +
                "(SELECT COUNT(*) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount, " +
                "($selectForPresence) as presence " +
                "FROM playlist " +
                "ORDER BY playlist.name COLLATE UNICODE")
            .toSQLiteQuery()
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

    fun getQueryForTagsCount(filterList: List<QueryFilter>) = ("SELECT " +
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


    fun getQueryForPodcastCount(filterList: List<QueryFilter>) = ("SELECT " +
            "COUNT(DISTINCT PodcastEpisode.id) AS podcastEpisodes " +
            "FROM PodcastEpisode" +
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
        shouldJoinSong: Boolean = false
    ): String {
        if (filterList.isNullOrEmpty() && orderingList.isEmpty()) {
            return " "
        }
        val orderingJoins = orderingList.mapNotNull { it.getJoin() }.toSet()
        val filterJoin = filterList?.flatMap { it.getJoins() }?.toSet() ?: emptySet()
        val playlistJoinSong =
            if (shouldJoinSong) setOf(
                QueryJoin(
                    QueryJoin.JoinType.PLAYLIST_SONG,
                    0
                )
            ) else emptySet()
        val joinsUnfiltered = orderingJoins + filterJoin + playlistJoinSong
        val joins = if (joinsUnfiltered.any { it.type == QueryJoin.JoinType.ALBUM_ARTIST }) {
            joinsUnfiltered.filterNot { it.type == QueryJoin.JoinType.ALBUM }
        } else {
            joinsUnfiltered
        }
        return joins.joinToString(separator = " ", prefix = " ") { it.getJoinClause() }
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
                where += constructWhereSubStatement(filterList)
            }
            where
        } else {
            ""
        }
    }

    private fun constructWhereSubStatement( //todo check parenthesis
        filterList: List<QueryFilter>
    ): String {
        var whereStatement = ""
        filterList
            .forEachIndexed { index, filter ->
                if (filter.children.isNotEmpty()) {
                    whereStatement += " ("
                }

                whereStatement += filter.getCondition()
                if (filter.children.isNotEmpty()) {
                    whereStatement += " AND" + constructWhereSubStatement(
                        filter.children
                    )
                    whereStatement += ")"
                }
                if (index < filterList.size - 1) {
                    whereStatement += " OR"
                }
            }
        return whereStatement
    }

    private fun constructOrderStatement(
        orderingList: List<QueryOrdering>
    ): String {
        val filteredOrderedList = orderingList
            .filter { it !is QueryOrdering.Precise }
            .sortedBy { it.priority }

        val isSorted = filteredOrderedList.isNotEmpty()
                && filteredOrderedList.none { it is QueryOrdering.Random }

        var orderStatement = if (isSorted) {
            " ORDER BY "
        } else {
            ""
        }

        if (isSorted) {
            orderStatement += filteredOrderedList.joinToString { it.getOrderingClause() }
        }

        return orderStatement
    }
}