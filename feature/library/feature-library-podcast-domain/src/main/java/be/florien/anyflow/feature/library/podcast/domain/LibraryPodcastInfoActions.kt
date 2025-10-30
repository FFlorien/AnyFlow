package be.florien.anyflow.feature.library.podcast.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.resources.R

enum class LibraryPodcastFieldType(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val artType: String
) {
    Podcast(R.drawable.ic_podcast, R.string.library_type_podcast, "podcast"),
    PodcastEpisode(R.drawable.ic_podcast_episode, R.string.library_type_podcast_episode, "podcast");
}

enum class LibraryPodcastActionType {
    SubFilter,
    InfoTitle;
}

data class LibraryInfoRow(
    val fieldType: LibraryPodcastFieldType,
    val actionType: LibraryPodcastActionType,
    val count: Int
)