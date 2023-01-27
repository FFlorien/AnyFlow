package be.florien.anyflow.feature.player.info.song

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.FragmentInfoBinding
import be.florien.anyflow.databinding.ItemDownloadInfoBinding
import be.florien.anyflow.databinding.ItemQuickActionInfoBinding
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoAdapter
import be.florien.anyflow.feature.player.info.InfoViewHolder
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

        private const val ITEM_VIEW_TYPE_DEFAULT = 0
        private const val ITEM_VIEW_TYPE_QUICK_ACTION = 1
        private const val ITEM_VIEW_TYPE_DOWNLOAD = 2
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
            ViewModelProvider(
                requireActivity(),
                (requireActivity() as ViewModelFactoryHolder).getFactory()
            )[QuickActionsViewModel::class.java]
                .apply {
                    val width = requireActivity().getDisplayWidth()
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        } else {
            ViewModelProvider(
                this,
                (requireActivity() as ViewModelFactoryHolder).getFactory()
            )[SongInfoViewModel::class.java]
        }
        if (parentFragment != null) {
            songListViewModel = ViewModelProvider(
                requireParentFragment(),
                (requireActivity() as ViewModelFactoryHolder).getFactory()
            )[SongListViewModel::class.java]
        }
        viewModel.song = song
        viewModel.songInfo.observe(this) {
            viewModel.updateRows()
        }
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songInfo.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songInfo.adapter = SongInfoAdapter(viewModel::executeAction)
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

    class SongInfoAdapter(private val executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit) :
        InfoAdapter<InfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            return when (viewType) {
                ITEM_VIEW_TYPE_QUICK_ACTION -> QuickActionInfoViewHolder(parent, executeAction)
                ITEM_VIEW_TYPE_DOWNLOAD -> DownloadInfoViewHolder(parent, executeAction)
                else -> InfoViewHolder(parent, executeAction)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is SongInfoActions.QuickActionInfoRow -> ITEM_VIEW_TYPE_QUICK_ACTION
                is SongInfoActions.SongDownloadInfoRow -> ITEM_VIEW_TYPE_DOWNLOAD
                else -> ITEM_VIEW_TYPE_DEFAULT
            }
        }
    }

    class QuickActionInfoViewHolder(
        parent: ViewGroup,
        executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit,
        private val parentBinding: ItemQuickActionInfoBinding = ItemQuickActionInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root) {
        override fun bindChangedData(row: InfoActions.InfoRow) {
            super.bindChangedData(row)
            if (row is SongInfoActions.QuickActionInfoRow) {
                parentBinding.order.removeViews(2, parentBinding.order.size - 2)
                val inflater = LayoutInflater.from(parentBinding.root.context)
                for (i in 0 until row.order) {
                    val unselectedView = inflater
                        .inflate(
                            R.layout.item_action_order,
                            parentBinding.order,
                            false
                        ) as ImageView
                    unselectedView.setImageResource(R.drawable.ic_action_order_item_unselected)
                    parentBinding.order.addView(unselectedView)
                }
            }
        }

        override fun setLifecycleOwner() {
            parentBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }
    }

    class DownloadInfoViewHolder(
        parent: ViewGroup,
        executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit,
        private val parentBinding: ItemDownloadInfoBinding = ItemDownloadInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root) {

        override fun bindChangedData(row: InfoActions.InfoRow) {
            super.bindChangedData(row)
            if (row is SongInfoActions.SongDownloadInfoRow) {
                parentBinding.display = row
            }
        }

        override fun setLifecycleOwner() {
            parentBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }
    }
}
