package be.florien.anyflow.feature.library.tags.ui.info

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoRow
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsActionType
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsFieldType
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.feature.library.tags.ui.list.LibraryTagsListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.TagFilterType
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LibraryTagsInfoFragment(parentFilter: Filter<*>? = null) :
    LibraryInfoFragment<LibraryInfoRow, TagFilterType>(parentFilter) {
    override fun getTitle(): String = getString(R.string.library_title_main)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()
    override fun getLibraryInfoViewModel() = ViewModelProvider(
        requireActivity(),
        requireActivity().viewModelFactory
    )[Random(23).toString(), LibraryTagsInfoViewModel::class.java]

    private fun getField(
        filterType: LibraryTagsFieldType
    ): TagFilterType {
        return when (filterType) {
            LibraryTagsFieldType.Song -> TagFilterType.SONG_IS
            LibraryTagsFieldType.Artist -> TagFilterType.ARTIST_IS
            LibraryTagsFieldType.AlbumArtist -> TagFilterType.ALBUM_ARTIST_IS
            LibraryTagsFieldType.Album -> TagFilterType.ALBUM_IS
            LibraryTagsFieldType.Playlist -> TagFilterType.PLAYLIST_IS
            LibraryTagsFieldType.Downloaded -> TagFilterType.DOWNLOADED_STATUS_IS
            LibraryTagsFieldType.Duration -> TagFilterType.SONG_IS
            LibraryTagsFieldType.Genre -> TagFilterType.SONG_IS
        }
    }

    override fun executeAction(row: LibraryInfoRow) {
        val action = row.actionType
        val field = row.fieldType
        when (action) {
            LibraryTagsActionType.SubFilter -> {
                val value = when (field) {
                    LibraryTagsFieldType.Playlist -> LibraryTagsInfoViewModel.PLAYLIST_ID
                    LibraryTagsFieldType.Album -> LibraryTagsInfoViewModel.ALBUM_ID
                    LibraryTagsFieldType.AlbumArtist -> LibraryTagsInfoViewModel.ALBUM_ARTIST_ID
                    LibraryTagsFieldType.Artist -> LibraryTagsInfoViewModel.ARTIST_ID
                    LibraryTagsFieldType.Genre -> LibraryTagsInfoViewModel.GENRE_ID
                    LibraryTagsFieldType.Song -> LibraryTagsInfoViewModel.SONG_ID
                    LibraryTagsFieldType.Downloaded -> LibraryTagsInfoViewModel.DOWNLOAD_ID
                    else -> LibraryTagsInfoViewModel.GENRE_ID
                }
                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryTagsListFragment(value, viewModel.filterNavigation),
                    "TAGS",
                    LibraryTagsListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }

    override suspend fun LibraryInfoRow.toInfoRow(): InfoRow {
        return when (this.actionType) {
            LibraryTagsActionType.InfoTitle -> {
                val idText = getIdText()

                val text = if (fieldType == LibraryTagsFieldType.Duration) {
                    TimeOperations.toMediaDuration(count.toDuration(DurationUnit.SECONDS), resources)
                } else {
                    idText.text
                }
                val imageUrl = this.fieldType
                    .artType
                    ?.let { artType ->
                        viewModel.getArtUrl(artType, idText.id)
                    }
                InfoRow.BasicInfoRow(
                    this.fieldType.titleRes,
                    TextConfig(text, null),
                    ImageConfig(imageUrl, fieldType.iconRes),
                    tag = this@toInfoRow
                )
            }

            LibraryTagsActionType.SubFilter -> {
                InfoRow.NavigationInfoRow(
                    this.fieldType.titleRes,
                    TextConfig(count.toString(), null),
                    ImageConfig(null, fieldType.iconRes),
                            tag = this@toInfoRow
                )
            }
        }
    }

    private suspend fun LibraryInfoRow.getIdText(): IdText {
        val filter = viewModel.filterNavigation
        val filterType = getField(this.fieldType)
        val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
        val filterData: IdText? = filterIfTypePresent?.takeIf { it.argument is Long }
            ?.let { IdText(it.argument as Long, it.displayText) }
        return filterData ?: (viewModel as LibraryTagsInfoViewModel).getFilteredInfo(
            filterType,
            filter
        ) ?: IdText(0, "")
    }
}
