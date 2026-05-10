package be.florien.anyflow.feature.library.tags.ui.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.tags.ui.list.LibraryTagsListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryActionType
import be.florien.anyflow.feature.library.ui.info.LibraryFieldType
import be.florien.anyflow.feature.library.ui.info.LibraryInfoRow
import be.florien.anyflow.feature.library.ui.info.LibraryInfoScreen
import be.florien.anyflow.management.filters.domain.model.Filter
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random

class LibraryTagsInfoFragment(val parentFilter: Filter? = null) : BaseFragment() {
    override fun getTitle(): String = getString(R.string.library_title_main)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()

    lateinit var viewModel: LibraryTagsInfoViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[Random(23).toString(), LibraryTagsInfoViewModel::class.java]
        viewModel.filterNavigation = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return ComposeView(requireActivity()).apply {
            setContent {
                val state = viewModel.state.collectAsStateWithLifecycle(persistentListOf())
                AppTheme {
                    LibraryInfoScreen(
                        state.value,
                        {
                            executeAction(viewModel.getInfoRow(it))
                        }
                    )
                }
            }
        }
    }

    fun executeAction(row: LibraryInfoRow) {
        val action = row.actionType
        if (row.fieldType !is LibraryFieldType.Tags) {
            return
        }
        when (action) {
            LibraryActionType.SubFilter -> {
                val value = when (row.fieldType) {
                    LibraryFieldType.Tags.Playlist -> LibraryTagsInfoViewModel.Companion.PLAYLIST_ID
                    LibraryFieldType.Tags.Album -> LibraryTagsInfoViewModel.Companion.ALBUM_ID
                    LibraryFieldType.Tags.AlbumArtist -> LibraryTagsInfoViewModel.Companion.ALBUM_ARTIST_ID
                    LibraryFieldType.Tags.Artist -> LibraryTagsInfoViewModel.Companion.ARTIST_ID
                    LibraryFieldType.Tags.Genre -> LibraryTagsInfoViewModel.Companion.GENRE_ID
                    LibraryFieldType.Tags.Song -> LibraryTagsInfoViewModel.Companion.SONG_ID
                    LibraryFieldType.Tags.Downloaded -> LibraryTagsInfoViewModel.Companion.DOWNLOAD_ID
                    else -> LibraryTagsInfoViewModel.Companion.GENRE_ID
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
}
