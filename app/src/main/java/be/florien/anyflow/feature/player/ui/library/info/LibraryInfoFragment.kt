package be.florien.anyflow.feature.player.ui.library.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.databinding.FragmentSelectFilterTypeBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.feature.player.ui.info.InfoActions
import be.florien.anyflow.feature.player.ui.info.InfoAdapter
import be.florien.anyflow.feature.player.ui.info.InfoViewHolder
import be.florien.anyflow.feature.player.ui.info.library.LibraryInfoActions
import be.florien.anyflow.feature.player.ui.library.BaseFilteringFragment
import be.florien.anyflow.feature.player.ui.library.LibraryViewModel
import be.florien.anyflow.feature.player.ui.library.cancelChanges
import be.florien.anyflow.feature.player.ui.library.list.LibraryListFragment
import kotlin.random.Random

class LibraryInfoFragment(private var parentFilter: Filter<*>? = null) : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.library_title_main)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    lateinit var viewModel: LibraryInfoViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            requireActivity(),
            requireActivity().viewModelFactory
        )[Random(23).toString(), LibraryInfoViewModel::class.java] //todo handle this gracefully !
        viewModel.filterNavigation = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterTypeBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        val infoAdapter = LibraryInfoAdapter(this::executeAction)
        fragmentBinding.filterList.adapter = infoAdapter
        viewModel.infoRows.observe(viewLifecycleOwner) {
            infoAdapter.submitList(it)
        }
        return fragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.filterNavigation == null) {
            viewModel.cancelChanges()
        }
    }

    private fun executeAction(row: InfoActions.InfoRow) {
        val action = row.actionType
        val field = row.fieldType
        when (action) {
            LibraryInfoActions.LibraryActionType.SubFilter -> {
                val value = when (field) {
                    LibraryInfoActions.LibraryFieldType.Playlist -> LibraryInfoViewModel.PLAYLIST_ID
                    LibraryInfoActions.LibraryFieldType.Album -> LibraryInfoViewModel.ALBUM_ID
                    LibraryInfoActions.LibraryFieldType.AlbumArtist -> LibraryInfoViewModel.ALBUM_ARTIST_ID
                    LibraryInfoActions.LibraryFieldType.Artist -> LibraryInfoViewModel.ARTIST_ID
                    LibraryInfoActions.LibraryFieldType.Genre -> LibraryInfoViewModel.GENRE_ID
                    LibraryInfoActions.LibraryFieldType.Song -> LibraryInfoViewModel.SONG_ID
                    LibraryInfoActions.LibraryFieldType.Downloaded -> LibraryInfoViewModel.DOWNLOAD_ID
                    LibraryInfoActions.LibraryFieldType.PodcastEpisode -> LibraryInfoViewModel.PODCAST_EPISODE_ID
                    else -> LibraryInfoViewModel.GENRE_ID
                }
                (activity as PlayerActivity).supportFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.container,
                        LibraryListFragment(value, viewModel.filterNavigation),
                        LibraryListFragment::class.java.simpleName
                    )
                    .addToBackStack(null)
                    .commit()
            }

            else -> viewModel.executeAction(row)
        }
    }


    class LibraryInfoAdapter(private val executeAction: (row: InfoActions.InfoRow) -> Unit) :
        InfoAdapter<InfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            return InfoViewHolder(parent, executeAction)
        }
    }
}
