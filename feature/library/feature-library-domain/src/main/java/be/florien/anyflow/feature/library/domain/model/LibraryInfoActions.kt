package be.florien.anyflow.feature.library.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.resources.R

sealed interface LibraryFieldType {
    @get:DrawableRes
    val iconRes: Int

    @get:StringRes
    val titleRes: Int
    val artType: String?

    enum class Tags(
        @param:DrawableRes override val iconRes: Int,
        @param:StringRes override val titleRes: Int,
        override val artType: String?
    ) : LibraryFieldType {
        Duration(R.drawable.ic_duration, R.string.filter_info_duration, null),
        Genre(R.drawable.ic_genre, R.string.filter_info_genre, null),
        AlbumArtist(R.drawable.ic_album_artist, R.string.filter_info_album_artist, ART_TYPE_ARTIST),
        Album(R.drawable.ic_album, R.string.filter_info_album, ART_TYPE_ALBUM),
        Artist(R.drawable.ic_artist, R.string.filter_info_artist, ART_TYPE_ARTIST),
        Song(R.drawable.ic_song, R.string.filter_info_song, ART_TYPE_SONG),
        Playlist(R.drawable.ic_playlist, R.string.filter_info_playlist, ART_TYPE_PLAYLIST),
        Downloaded(R.drawable.ic_downloaded, R.string.filter_info_downloaded, null);
    }

    enum class Podcast(
        @param:DrawableRes override val iconRes: Int,
        @param:StringRes override val titleRes: Int,
        override val artType: String?
    ) : LibraryFieldType {
        Podcast(R.drawable.ic_podcast, R.string.library_type_podcast, "podcast"),
        PodcastEpisode(
            R.drawable.ic_podcast_episode,
            R.string.library_type_podcast_episode,
            "podcast"
        );
    }
}

enum class LibraryRowType(
    val isSingleDbRow: Boolean,
    val isUsingArt: Boolean,
    val hasLeftIcon: Boolean,
    @param:DrawableRes val iconRes: Int?,
    @param:StringRes val titleRes: Int?,
    @param:StringRes val descriptionRes: Int?
) {

    // multirow
    SubFilter(
        isSingleDbRow = false,
        isUsingArt = false,
        hasLeftIcon = true,
        iconRes = R.drawable.ic_go,
        titleRes = null,
        descriptionRes = null
    ),
    InfoTitle(
        isSingleDbRow = false,
        isUsingArt = false,
        hasLeftIcon = true,
        iconRes = null,
        titleRes = null,
        descriptionRes = null
    ),
    //singlerow
    ExpandableTitle(
        isSingleDbRow = true,
        isUsingArt = true,
        hasLeftIcon = true,
        iconRes = R.drawable.ic_next_occurence,
        titleRes = null,
        descriptionRes = null
    ),
    ExpandedTitle(
        isSingleDbRow = true,
        isUsingArt = true,
        hasLeftIcon = true,
        iconRes = R.drawable.ic_previous_occurence,
        titleRes = null,
        descriptionRes = null
    ),

    //action
    AddToFilter(
        isSingleDbRow = true,
        isUsingArt = false,
        hasLeftIcon = false,
        iconRes = R.drawable.ic_filter,
        titleRes = R.string.info_action_filter_title,
        descriptionRes = R.string.info_action_filter_on
    ),
    AddToPlaylist(
        isSingleDbRow = true,
        isUsingArt = false,
        hasLeftIcon = false,
        iconRes = R.drawable.ic_add_to_playlist,
        titleRes = R.string.info_action_select_playlist,
        descriptionRes = R.string.info_action_select_playlist_detail
    ),
    AddNext(
        isSingleDbRow = true,
        isUsingArt = false,
        hasLeftIcon = false,
        iconRes = R.drawable.ic_play_next,
        titleRes = R.string.info_action_next_title,
        descriptionRes = R.string.info_action_track_next
    ),
    Search(
        isSingleDbRow = true,
        isUsingArt = false,
        hasLeftIcon = false,
        iconRes = R.drawable.ic_search,
        titleRes = R.string.info_action_search_title,
        descriptionRes = R.string.info_action_search_on
    ),
    Download(
        isSingleDbRow = true,
        isUsingArt = false,
        hasLeftIcon = false,
        iconRes = R.drawable.ic_download,
        titleRes = R.string.info_action_download,
        descriptionRes = R.string.info_action_download_description
    );
}

const val ART_TYPE_SONG = "song"
const val ART_TYPE_ALBUM = "album"
const val ART_TYPE_ARTIST = "artist"
const val ART_TYPE_PLAYLIST = "playlist"