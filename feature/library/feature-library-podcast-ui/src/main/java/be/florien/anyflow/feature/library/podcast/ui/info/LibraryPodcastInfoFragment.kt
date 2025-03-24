package be.florien.anyflow.feature.library.podcast.ui.info

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.library.podcast.domain.LibraryInfoRow
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastActionType
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastFieldType
import be.florien.anyflow.feature.library.podcast.ui.list.LibraryPodcastListFragment
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import kotlin.random.Random

class LibraryPodcastInfoFragment(parentFilterParam: Filter? = null) :
    LibraryInfoFragment<LibraryInfoRow, PodcastFilterType>(parentFilterParam) {
    override fun getTitle(): String = getString(R.string.menu_podcast)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()
    override fun getLibraryInfoViewModel() = ViewModelProvider(
        requireActivity(),
        requireActivity().viewModelFactory
    )[Random(23).toString(), LibraryPodcastInfoViewModel::class.java]

    override fun executeAction(row: LibraryInfoRow) {
        val action = row.actionType
        when (action) {
            LibraryPodcastActionType.SubFilter -> {
                val value = when (row.fieldType) {
                    LibraryPodcastFieldType.Podcast -> LibraryPodcastInfoViewModel.PODCAST_ID
                    LibraryPodcastFieldType.PodcastEpisode -> LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID
                }

                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryPodcastListFragment(value, viewModel.filterNavigation),
                    "PODCAST",
                    LibraryPodcastListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }

    override suspend fun LibraryInfoRow.toInfoRow(): InfoRow {
        return when (this.actionType) { //todo get the correct image: id is for episode, but podcast is needed
            LibraryPodcastActionType.InfoTitle -> {
                val idText = getIdText()
                val imageUrl = viewModel
                    .getArtUrl(
                        this.fieldType.artType,
                        idText.id
                    )
                InfoRow.BasicInfoRow(
                    this.fieldType.titleRes,
                    TextConfig(idText.text, null),
                    ImageConfig(imageUrl, fieldType.iconRes),
                    tag = this
                )
            }

            LibraryPodcastActionType.SubFilter -> InfoRow.NavigationInfoRow(
                this.fieldType.titleRes,
                TextConfig(count.toString(), null),
                ImageConfig(null, fieldType.iconRes),
                tag = this
            )
        }
    }

    private suspend fun LibraryInfoRow.getIdText(): IdText {
        val filter = viewModel.filterNavigation
        val filterType = when (fieldType) {
            LibraryPodcastFieldType.Podcast -> PodcastFilterType.PODCAST_IS
            LibraryPodcastFieldType.PodcastEpisode -> PodcastFilterType.PODCAST_EPISODE_IS
        }
        val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
        val filterData: IdText? = filterIfTypePresent?.takeIf { it.argument is Long }
            ?.let { IdText(it.argument as Long, it.displayText) }
        return filterData ?: (viewModel as LibraryPodcastInfoViewModel).getFilteredInfo(
            filterType,
            filter
        ) ?: IdText(0, "")
    }
}
