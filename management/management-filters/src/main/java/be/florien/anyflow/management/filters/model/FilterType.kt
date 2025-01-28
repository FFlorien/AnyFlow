package be.florien.anyflow.management.filters.model

sealed interface FilterType {
    val artType: String?
}

enum class TagFilterType(override val artType: String?): FilterType {
    SONG_IS(Filter.ART_TYPE_SONG),
    ARTIST_IS(Filter.ART_TYPE_ARTIST),
    ALBUM_ARTIST_IS(Filter.ART_TYPE_ARTIST),
    ALBUM_IS(Filter.ART_TYPE_ALBUM),
    GENRE_IS(null),
    PLAYLIST_IS(Filter.ART_TYPE_PLAYLIST),
    DOWNLOADED_STATUS_IS(null),
    DISK_IS(Filter.ART_TYPE_ALBUM)
}

enum class PodcastFilterType(override val artType: String?): FilterType {
    PODCAST_EPISODE_IS(Filter.ART_TYPE_PODCAST),
    PODCAST_IS(Filter.ART_TYPE_PODCAST),
    STATE_IS(null)
}