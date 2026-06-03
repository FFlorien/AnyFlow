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
        Duration(
            iconRes = R.drawable.ic_duration,
            titleRes = R.string.filter_info_duration,
            artType = null
        ),
        Genre(
            iconRes = R.drawable.ic_genre,
            titleRes = R.string.filter_info_genre,
            artType = null
        ),
        AlbumArtist(
            iconRes = R.drawable.ic_album_artist,
            titleRes = R.string.filter_info_album_artist,
            artType = ART_TYPE_ARTIST
        ),
        Album(
            iconRes = R.drawable.ic_album,
            titleRes = R.string.filter_info_album,
            artType = ART_TYPE_ALBUM
        ),
        Artist(
            iconRes = R.drawable.ic_artist,
            titleRes = R.string.filter_info_artist,
            artType = ART_TYPE_ARTIST
        ),
        Song(
            iconRes = R.drawable.ic_song,
            titleRes = R.string.filter_info_song,
            artType = ART_TYPE_SONG
        ),
        Playlist(
            iconRes = R.drawable.ic_playlist,
            titleRes = R.string.filter_info_playlist,
            artType = ART_TYPE_PLAYLIST
        ),
        Downloaded(
            iconRes = R.drawable.ic_downloaded,
            titleRes = R.string.filter_info_downloaded,
            artType = null
        );
    }

    enum class Podcast(
        @param:DrawableRes override val iconRes: Int,
        @param:StringRes override val titleRes: Int,
        override val artType: String?
    ) : LibraryFieldType {
        Podcast(
            iconRes = R.drawable.ic_podcast,
            titleRes = R.string.library_type_podcast,
            artType = ART_TYPE_PODCAST
        ),
        PodcastEpisode(
            iconRes = R.drawable.ic_podcast_episode,
            titleRes = R.string.library_type_podcast_episode,
            artType = ART_TYPE_PODCAST
        );
    }
}

sealed interface LibraryRowType {
    @get:DrawableRes
    val iconRes: Int?

    @get:StringRes
    val titleRes: Int?

    @get:StringRes
    val descriptionRes: Int?

    enum class MultiRow(
        override val iconRes: Int?,
    ) : LibraryRowType {

        SubFilter(iconRes = R.drawable.ic_go),
        InfoTitle(iconRes = null);

        override val titleRes = null
        override val descriptionRes = null
    }

    enum class SingleRow(override val iconRes: Int?) : LibraryRowType {
        ExpandableTitle(iconRes = R.drawable.ic_next_occurence),
        ExpandedTitle(iconRes = R.drawable.ic_previous_occurence);

        override val titleRes = null
        override val descriptionRes = null
    }

    enum class Action(
        override val iconRes: Int?,
        override val titleRes: Int?,
        override val descriptionRes: Int?
    ) : LibraryRowType {
        SeeInLibrary(
            iconRes = R.drawable.ic_library,
            titleRes = R.string.info_action_see_in_library,
            descriptionRes = R.string.info_action_see_in_library_detail
        ),
        AddToFilter(
            iconRes = R.drawable.ic_filter,
            titleRes = R.string.info_action_filter_title,
            descriptionRes = R.string.info_action_filter_on
        ),
        AddToPlaylist(
            iconRes = R.drawable.ic_add_to_playlist,
            titleRes = R.string.info_action_select_playlist,
            descriptionRes = R.string.info_action_select_playlist_detail
        ),
        AddNext(
            iconRes = R.drawable.ic_play_next,
            titleRes = R.string.info_action_next_title,
            descriptionRes = R.string.info_action_track_next
        ),
        Search(
            iconRes = R.drawable.ic_search,
            titleRes = R.string.info_action_search_title,
            descriptionRes = R.string.info_action_search_on
        ),
        Download(
            iconRes = R.drawable.ic_download,
            titleRes = R.string.info_action_download,
            descriptionRes = R.string.info_action_download_description
        );

    }
}

fun LibraryRowType.getKey() =
    if (this is LibraryRowType.SingleRow) {
        "expandable"
    } else {
        when(this) {
            is LibraryRowType.Action -> this.name
            is LibraryRowType.MultiRow -> this.name
        }
    }


const val ART_TYPE_SONG = "song"
const val ART_TYPE_ALBUM = "album"
const val ART_TYPE_ARTIST = "artist"
const val ART_TYPE_PLAYLIST = "playlist"
const val ART_TYPE_PODCAST = "podcast"