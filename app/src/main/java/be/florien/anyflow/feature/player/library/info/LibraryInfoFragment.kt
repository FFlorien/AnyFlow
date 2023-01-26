package be.florien.anyflow.feature.player.library.info

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
import be.florien.anyflow.feature.menu.implementation.FilterMenuHolder
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoAdapter
import be.florien.anyflow.feature.player.info.InfoViewHolder
import be.florien.anyflow.feature.player.library.BaseFilteringFragment
import be.florien.anyflow.feature.player.library.LibraryViewModel
import be.florien.anyflow.feature.player.library.cancelChanges
import be.florien.anyflow.feature.player.library.filters.DisplayFilterFragment
import be.florien.anyflow.feature.player.library.list.LibraryListFragment
import kotlin.random.Random

class LibraryInfoFragment(private var parentFilter: Filter<*>? = null) : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.library_title_main)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    lateinit var viewModel: LibraryInfoViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding
    private lateinit var filterMenu: FilterMenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterMenu = FilterMenuHolder {
            displayFilters()
        }
        menuCoordinator.addMenuHolder(filterMenu)
        filterMenu.isVisible = parentFilter == null
    }

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
        filterMenu.isVisible = false
        if (viewModel.filterNavigation == null) {
            viewModel.cancelChanges()
        }
        menuCoordinator.removeMenuHolder(filterMenu)
    }

    private fun displayFilters() {
        val fragment =
            requireActivity().supportFragmentManager.findFragmentByTag(DisplayFilterFragment::class.java.simpleName)
                ?: DisplayFilterFragment()
        requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.slide_backward,
                R.anim.slide_forward,
                R.anim.slide_out_top
            )
            .replace(R.id.container, fragment, DisplayFilterFragment::class.java.simpleName)
            .addToBackStack(null)
            .commit()
    }

    private fun executeAction(field: InfoActions.FieldType, action: InfoActions.ActionType) {
        when (action) {
            is InfoActions.LibraryActionType.SubFilter -> {
                val value = when (field) {
                    is InfoActions.LibraryFieldType.Playlist -> LibraryInfoViewModel.PLAYLIST_ID
                    is InfoActions.LibraryFieldType.Album -> LibraryInfoViewModel.ALBUM_ID
                    is InfoActions.LibraryFieldType.AlbumArtist -> LibraryInfoViewModel.ALBUM_ARTIST_ID
                    is InfoActions.LibraryFieldType.Artist -> LibraryInfoViewModel.ARTIST_ID
                    is InfoActions.LibraryFieldType.Genre -> LibraryInfoViewModel.GENRE_ID
                    is InfoActions.LibraryFieldType.Song -> LibraryInfoViewModel.SONG_ID
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
            else -> viewModel.executeAction(field, action)
        }
    }


    class LibraryInfoAdapter(private val executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit) :
        InfoAdapter<InfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            return InfoViewHolder(parent, executeAction)
        }
    }
}
