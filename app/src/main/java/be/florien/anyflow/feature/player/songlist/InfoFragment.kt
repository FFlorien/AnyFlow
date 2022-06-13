package be.florien.anyflow.feature.player.songlist

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.FragmentInfoBinding
import be.florien.anyflow.databinding.ItemInfoBinding
import be.florien.anyflow.feature.quickActions.InfoActionsSelectionViewModel
import be.florien.anyflow.injection.ViewModelFactoryHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InfoFragment(
    private var song: SongInfo = SongInfo(
        SongInfoActions.DUMMY_SONG_ID, "", "", 0L, "", 0L, "", 0, listOf(""), 0, 0, 0, ""
    )
) : BottomSheetDialogFragment() {

    companion object {
        private const val SONG = "SONG"
    }

    private lateinit var viewModel: InfoViewModel
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
            ViewModelProvider(requireActivity(), (requireActivity() as ViewModelFactoryHolder).getFactory())
                .get(InfoActionsSelectionViewModel::class.java)
                .apply {
                    val displayMetrics = DisplayMetrics()
                    requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                    val width = displayMetrics.widthPixels
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        } else {
            ViewModelProvider(this, (requireActivity() as ViewModelFactoryHolder).getFactory())
                .get(InfoDisplayViewModel::class.java)
        }
        if (parentFragment != null) {
            songListViewModel = ViewModelProvider(
                requireParentFragment(),
                (requireActivity() as ViewModelFactoryHolder).getFactory()
            ).get(SongListViewModel::class.java)
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
        binding.songInfo.adapter = InfoAdapter()
        viewModel.songRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it)
        }
        if (songListViewModel != null) {
            viewModel.searchTerm.observe(viewLifecycleOwner) {
                if (it != null) {
                    songListViewModel?.isSearching?.value = true
                    songListViewModel?.searchedText?.value = it
                    dismiss()
                }
            }
        }
        viewModel.isPlaylistListDisplayed.observe(viewLifecycleOwner) {
            if (it) {
                SelectPlaylistFragment(song.id).show(childFragmentManager, null)
            }
        }
    }

    inner class InfoAdapter : ListAdapter<SongInfoActions.SongRow, InfoViewHolder>(object :
        DiffUtil.ItemCallback<SongInfoActions.SongRow>() {
        override fun areItemsTheSame(
            oldItem: SongInfoActions.SongRow,
            newItem: SongInfoActions.SongRow
        ): Boolean {
            val isSameAction = (oldItem.actionType == newItem.actionType)
                    || (oldItem.actionType == SongInfoActions.ActionType.EXPANDED_TITLE && newItem.actionType == SongInfoActions.ActionType.EXPANDABLE_TITLE)
                    || (oldItem.actionType == SongInfoActions.ActionType.EXPANDABLE_TITLE && newItem.actionType == SongInfoActions.ActionType.EXPANDED_TITLE)
            return oldItem.fieldType == newItem.fieldType && isSameAction
        }

        override fun areContentsTheSame(
            oldItem: SongInfoActions.SongRow,
            newItem: SongInfoActions.SongRow
        ) = areItemsTheSame(
            oldItem,
            newItem
        ) && oldItem.actionType == newItem.actionType && oldItem.order == newItem.order
    }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder =
            InfoViewHolder(parent)

        override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
            holder.bindNewData(getItem(position))
        }

        override fun onBindViewHolder(
            holder: InfoViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                holder.bindChangedData(getItem(position))
            }
        }
    }

    inner class InfoViewHolder(
        val parent: ViewGroup,
        val binding: ItemInfoBinding = ItemInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindNewData(row: SongInfoActions.SongRow) {
            bindChangedData(row)
            binding.display = row
            binding.descriptionText = when {
                row.text == null && row.textRes != null -> parent.context.resources.getString(row.textRes)
                row.text != null && row.textRes == null -> row.text
                row.text != null && row.textRes != null -> parent.context.resources.getString(
                    row.textRes,
                    row.text
                )
                else -> ""
            }
            binding.root.setOnClickListener {
                viewModel.executeAction(row.fieldType, row.actionType)
            }
            if (row.actionType == SongInfoActions.ActionType.NONE) {
                binding.root.setBackgroundColor(
                    ResourcesCompat.getColor(
                        parent.context.resources,
                        R.color.primaryBackground,
                        parent.context.theme
                    )
                )
            } else if (row.actionType != SongInfoActions.ActionType.INFO_TITLE && row.actionType != SongInfoActions.ActionType.EXPANDABLE_TITLE && row.actionType != SongInfoActions.ActionType.EXPANDED_TITLE) {
                binding.root.setBackgroundColor(
                    ResourcesCompat.getColor(
                        parent.context.resources,
                        R.color.accentBackground,
                        parent.context.theme
                    )
                )
            } else {
                binding.root.setBackgroundColor(0)
            }
        }

        fun bindChangedData(row: SongInfoActions.SongRow) {
            if (row.order != null) {
                binding.order.removeViews(2, binding.order.size - 2)
                val inflater = LayoutInflater.from(binding.root.context)
                for (i in 0 until row.order) {
                    val unselectedView = inflater
                        .inflate(R.layout.item_action_order, binding.order, false) as ImageView
                    unselectedView.setImageResource(R.drawable.ic_action_order_item_unselected)
                    binding.order.addView(unselectedView)
                }
            }
        }
    }
}
