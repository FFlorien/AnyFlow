package be.florien.anyflow.feature.library.podcast.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.common.resources.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastInfoActions @Inject constructor(
    private val libraryPodcastRepository: LibraryPodcastRepository
) : InfoActions<Filter<*>?>() {

    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        val count =
            withContext(Dispatchers.IO) { libraryPodcastRepository.getFilteredInfo(infoSource) }
        return listOfNotNull(
            getInfoRow(
                R.string.filter_info_podcast_episodes,
                infoSource,
                Filter.FilterType.PODCAST_EPISODE_IS,
                count.podcastEpisodes //todo number of podcast episodes + move to another screen
            )
        )
    }

    override fun getActionsRows(
        infoSource: Filter<*>?,
        row: InfoRow
    ): List<InfoRow> = emptyList()

    private suspend fun getInfoRow(
        @StringRes title: Int,
        filter: Filter<*>?,
        filterType: Filter.FilterType,
        count: Int
    ): InfoRow {
        val displayData: DisplayData? = if (count <= 1) {
            val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
            val filterData: DisplayData? =
                filterIfTypePresent?.let { DisplayData(it.displayText, it.argument as Long) }
            filterData ?: when (filterType) {
                Filter.FilterType.PODCAST_EPISODE_IS -> libraryPodcastRepository.getPodcastEpisodeList(
                    null
                )

                else -> listOf(null)
            }.firstOrNull()
        } else null

        val url = if (count <= 1 && displayData != null) {
            libraryPodcastRepository.getArtUrl(filter?.type?.artType, displayData.argument)
        } else {
            null
        }

        return LibraryInfoRow(
            title,
            getDisplayText(displayData, count),
            null,
            getField(),
            getAction(count),
            url
        )
    }

    private fun getDisplayText(
        filter: DisplayData?,
        count: Int
    ): String {
        return if (count <= 1 && filter != null) {
            filter.text
        } else {
            count.toString()
        }
    }

    private fun getField(): LibraryPodcastFieldType {
        return LibraryPodcastFieldType.PodcastEpisode
    }

    private fun getAction(count: Int): LibraryPodcastActionType {
        return if (count > 1) LibraryPodcastActionType.SubFilter else LibraryPodcastActionType.InfoTitle
    }

    class DisplayData(val text: String, val argument: Long)

    enum class LibraryPodcastFieldType(
        @DrawableRes override val iconRes: Int
    ) : FieldType {
        PodcastEpisode(R.drawable.ic_podcast_episode);
    }

    enum class LibraryPodcastActionType(
        @DrawableRes override val iconRes: Int,
        override val category: ActionTypeCategory
    ) : ActionType {
        SubFilter(R.drawable.ic_go, ActionTypeCategory.Navigation),
        InfoTitle(0, ActionTypeCategory.None);
    }

    data class LibraryInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: LibraryPodcastActionType,
        override val imageUrl: String?
    ) : InfoRow(title, text, textRes, fieldType, actionType, imageUrl)
}