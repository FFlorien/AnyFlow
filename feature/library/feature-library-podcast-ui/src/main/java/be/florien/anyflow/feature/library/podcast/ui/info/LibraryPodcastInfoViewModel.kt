package be.florien.anyflow.feature.library.podcast.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.component.info.R
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastRepository
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.InfoRowDisplay
import be.florien.anyflow.feature.library.ui.info.LibraryActionType
import be.florien.anyflow.feature.library.ui.info.LibraryFieldType
import be.florien.anyflow.feature.library.ui.info.LibraryInfoRow
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastInfoViewModel @Inject constructor(
    private val libraryPodcastRepository: LibraryPodcastRepository,
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : ViewModel(), LibraryViewModel {
    private val mutableLibraryInfoRows: MutableStateFlow<List<LibraryInfoRow>> =
        MutableStateFlow(listOf())
    private val libraryInfoRows: StateFlow<List<LibraryInfoRow>> = mutableLibraryInfoRows

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter? = null
        set(value) {
            field = value
            updateRows()
        }

    val state: Flow<PersistentList<InfoRowDisplay>> = libraryInfoRows.map { list ->
        list
            .map {
                it.toInfoRow()
            }
            .toPersistentList()
    }

    fun executeAction(row: LibraryInfoRow): Boolean {
        return false
    }

    fun getInfoRow(position: Int): LibraryInfoRow = libraryInfoRows.value[position]

    private fun updateRows() {
        viewModelScope.launch {
            mutableLibraryInfoRows.value = getInfoRowList()
        }
    }

    private suspend fun getInfoRowList(): MutableList<LibraryInfoRow> {
        val count =
            withContext(Dispatchers.IO) { libraryPodcastRepository.getFilteredInfo(filterNavigation) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryFieldType.Podcast.Podcast,
                getAction(count.podcasts),
                count.podcasts
            ),
            LibraryInfoRow(
                LibraryFieldType.Podcast.PodcastEpisode,
                getAction(count.podcastEpisodes),
                count.podcastEpisodes
            )
        )
    }

    private fun getAction(count: Int): LibraryActionType {
        return if (count > 1) LibraryActionType.SubFilter else LibraryActionType.InfoTitle
        TODO("More Actions")
    }

    private suspend fun LibraryInfoRow.toInfoRow(): InfoRowDisplay {
        return when (this.actionType) { //todo get the correct image: id is for episode, but podcast is needed
            LibraryActionType.InfoTitle -> {
                val idText = getIdText()
                val imageUrl = this.fieldType.artType?.let { artType ->
                    libraryPodcastRepository.getArtUrl(artType, idText.id)
                }
                InfoRowDisplay(
                    ImageConfig(imageUrl, fieldType.iconRes),
                    this.fieldType.titleRes,
                    TextConfig(idText.text, null),
                    null
                )
            }

            LibraryActionType.SubFilter -> InfoRowDisplay(
                ImageConfig(null, fieldType.iconRes),
                this.fieldType.titleRes,
                TextConfig(count.toString(), null),
                R.drawable.ic_go
            )
        }
    }

    private suspend fun LibraryInfoRow.getIdText(): IdText {
        if (fieldType !is LibraryFieldType.Podcast) {
            return IdText(0, "")
        }
        val filter = filterNavigation
        val filterType = getField(fieldType as LibraryFieldType.Podcast)
        val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
        val filterData: IdText? = filterIfTypePresent?.takeIf { it.argument is Long }
            ?.let { IdText(it.argument as Long, it.displayText) }
        return filterData ?: getFilteredInfo(
            filterType,
            filter
        ) ?: IdText(0, "")
    }

    private fun getField(
        filterType: LibraryFieldType.Podcast
    ): PodcastFilterType {
        return when (filterType) {
            LibraryFieldType.Podcast.Podcast -> PodcastFilterType.PODCAST_IS
            LibraryFieldType.Podcast.PodcastEpisode -> PodcastFilterType.PODCAST_EPISODE_IS
        }
    }

    private suspend fun getFilteredInfo(
        filterType: PodcastFilterType,
        filter: Filter?
    ) = when (filterType) {
        PodcastFilterType.PODCAST_IS -> libraryPodcastRepository.getPodcastList(filter)
        PodcastFilterType.PODCAST_EPISODE_IS -> libraryPodcastRepository.getPodcastEpisodeList(
            filter
        )

        PodcastFilterType.STATE_IS -> emptyList()//todo
    }.firstOrNull()

    companion object {
        const val PODCAST_ID = "Podcast"
        const val PODCAST_EPISODE_ID = "PodcastEpisode"
    }
}