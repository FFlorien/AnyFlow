package be.florien.anyflow.feature.player.filter.selectType

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterTypeBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.feature.player.filter.selection.SelectFilterFragment
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoAdapter

class SelectFilterTypeFragment : BaseFilterFragment() {
    override fun getTitle(): String = getString(R.string.filter_title_main)
    override val filterActions: FilterActions
        get() = viewModel
    lateinit var viewModel: SelectFilterTypeViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            requireActivity(),
            requireActivity().viewModelFactory
        )[SelectFilterTypeViewModel::class.java]
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

    override fun onResume() {
        super.onResume()
        viewModel.filter = null //todo
    }

    private fun executeAction(field: InfoActions.FieldType, action: InfoActions.ActionType) {
        when(action) {
            is InfoActions.FilterActionType.SubFilter -> {
                val value = when (field) {
                    is InfoActions.FilterFieldType.Playlist -> SelectFilterTypeViewModel.PLAYLIST_ID
                    is InfoActions.FilterFieldType.Album -> SelectFilterTypeViewModel.ALBUM_ID
                    is InfoActions.FilterFieldType.AlbumArtist -> SelectFilterTypeViewModel.ARTIST_ID
                    // is InfoActions.FilterFieldType.Artist -> SelectFilterTypeViewModel.ARTIST_ID //todo
                    is InfoActions.FilterFieldType.Genre -> SelectFilterTypeViewModel.GENRE_ID
                    is InfoActions.FilterFieldType.Song -> SelectFilterTypeViewModel.SONG_ID
                    else -> SelectFilterTypeViewModel.GENRE_ID
                }
                (activity as PlayerActivity).supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, SelectFilterFragment(value), SelectFilterFragment::class.java.simpleName)
                    .addToBackStack(null)
                    .commit()
            }
            else -> viewModel.executeAction(field, action)
        }
    }
}
