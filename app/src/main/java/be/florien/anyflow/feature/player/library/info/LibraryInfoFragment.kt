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
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.library.BaseFilteringFragment
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.feature.player.library.list.LibraryListFragment
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoAdapter
import kotlin.random.Random

class LibraryInfoFragment(private var parentFilter: Filter<*>? = null) : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.filter_title_main)
    override val libraryActions: LibraryActions
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
        val infoAdapter = InfoAdapter(this::executeAction)
        fragmentBinding.filterList.adapter = infoAdapter
        viewModel.infoRows.observe(viewLifecycleOwner) {
            infoAdapter.submitList(it)
        }
        return fragmentBinding.root
    }

    private fun executeAction(field: InfoActions.FieldType, action: InfoActions.ActionType) {
        when (action) {
            is InfoActions.FilterActionType.SubFilter -> {
                val value = when (field) {
                    is InfoActions.FilterFieldType.Playlist -> LibraryInfoViewModel.PLAYLIST_ID
                    is InfoActions.FilterFieldType.Album -> LibraryInfoViewModel.ALBUM_ID
                    is InfoActions.FilterFieldType.AlbumArtist -> LibraryInfoViewModel.ALBUM_ARTIST_ID
                    is InfoActions.FilterFieldType.Artist -> LibraryInfoViewModel.ARTIST_ID
                    is InfoActions.FilterFieldType.Genre -> LibraryInfoViewModel.GENRE_ID
                    is InfoActions.FilterFieldType.Song -> LibraryInfoViewModel.SONG_ID
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
}
