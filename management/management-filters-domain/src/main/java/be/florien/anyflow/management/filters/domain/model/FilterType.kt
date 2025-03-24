package be.florien.anyflow.management.filters.domain.model

sealed interface FilterType {
    val artType: String?
}

enum class TagFilterType(override val artType: String?): FilterType {
    SONG_IS(FilterParam.ART_TYPE_SONG),
    ARTIST_IS(FilterParam.ART_TYPE_ARTIST),
    ALBUM_ARTIST_IS(FilterParam.ART_TYPE_ARTIST),
    ALBUM_IS(FilterParam.ART_TYPE_ALBUM),
    GENRE_IS(null),
    PLAYLIST_IS(FilterParam.ART_TYPE_PLAYLIST),
    DOWNLOADED_STATUS_IS(null),
    DISK_IS(FilterParam.ART_TYPE_ALBUM)
}

enum class PodcastFilterType(override val artType: String?): FilterType {
    PODCAST_EPISODE_IS(FilterParam.ART_TYPE_PODCAST),
    PODCAST_IS(FilterParam.ART_TYPE_PODCAST),
    STATE_IS(null)
}