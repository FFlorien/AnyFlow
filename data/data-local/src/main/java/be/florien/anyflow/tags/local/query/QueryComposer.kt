package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery

interface QueryComposer {
    //region tags
    fun getQueryForSongIds(
        filters: List<QueryFilter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery

    fun getQueryForSong(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForAlbum(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForAlbumArtist(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForArtist(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForGenre(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForSongCount(filter: QueryFilter): SimpleSQLiteQuery
    fun getQueryForTagsCount(filterList: List<QueryFilter>): SimpleSQLiteQuery

    //region playlist
    fun getQueryForPlaylist(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForPlaylistWithCount(
        filterList: List<QueryFilter>?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForPlaylistWithPresence(filter: QueryFilter): SimpleSQLiteQuery

    //region podcasts
    fun getQueryForPodcasts(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastEpisodes(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastEpisodeIds(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastCount(filterList: List<QueryFilter>): SimpleSQLiteQuery

    //region download
    fun getQueryForDownload(filterList: List<QueryFilter>?): SimpleSQLiteQuery
    fun getQueryForDownloadCount(filterList: List<QueryFilter>?): SimpleSQLiteQuery
    fun getQueryForDownloadProgress(filterList: List<QueryFilter>?): SimpleSQLiteQuery
}