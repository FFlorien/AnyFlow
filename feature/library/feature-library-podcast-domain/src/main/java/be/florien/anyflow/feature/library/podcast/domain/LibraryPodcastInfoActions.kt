package be.florien.anyflow.feature.library.podcast.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.resources.R

enum class LibraryPodcastFieldType(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    val artType: String
) {
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