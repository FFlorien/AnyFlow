package be.florien.anyflow.feature.player.info.song

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.FragmentInfoBinding
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.feature.player.info.InfoAdapter
import be.florien.anyflow.feature.player.info.song.quickActions.QuickActionsViewModel
import be.florien.anyflow.feature.player.songlist.SongListViewModel
import be.florien.anyflow.feature.playlist.selection.SelectPlaylistFragment
import be.florien.anyflow.injection.ViewModelFactoryHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongInfoFragment(
    private var song: SongInfo = SongInfo.dummySongInfo(SongInfoActions.DUMMY_SONG_ID)
) : BottomSheetDialogFragment() {

    companion object {
        private const val SONG = "SONG"
    }

    private lateinit var viewModel: BaseSongViewModel
    private var songListViewModel: SongListViewModel? = null
    private lateinit var binding: FragmentInfoBinding

    init {
        arguments?.let {
            song = it.getParcelable(SONG) ?: song
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putParcelable(SONG, song)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = if (song.id == SongInfoActions.DUMMY_SONG_ID) {
            ViewModelProvider(requireActivity(), (requireActivity() as ViewModelFactoryHolder).getFactory())[QuickActionsViewModel::class.java]
                .apply {
                    val width = requireActivity().getDisplayWidth()
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        } else {
            ViewModelProvider(this, (requireActivity() as ViewModelFactoryHolder).getFactory())[SongInfoViewModel::class.java]
        }
        if (parentFragment != null) {
            songListViewModel = ViewModelProvider(
                requireParentFragment(),
                (requireActivity() as ViewModelFactoryHolder).getFactory()
            )[SongListViewModel::class.java]
        }
        viewModel.song = song
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songInfo.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songInfo.adapter = InfoAdapter(viewModel::executeAction)
        viewModel.infoRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it)
        }
        (viewModel as? SongInfoViewModel)?.apply {
            if (songListViewModel != null) {
                searchTerm.observe(viewLifecycleOwner) {
                    if (it != null) {
                        songListViewModel?.isSearching?.value = true
                        songListViewModel?.searchedText?.value = it
                        dismiss()
                    }
                }
            }
            isPlaylistListDisplayed.observe(viewLifecycleOwner) {
                if (it) {
                    SelectPlaylistFragment(song.id).show(childFragmentManager, null)
                }
            }
        }
    }
}
