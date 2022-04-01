package be.florien.anyflow.feature.player.songlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.databinding.FragmentInfoBinding
import be.florien.anyflow.databinding.ItemInfoBinding
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.quickOptions.InfoOptionsSelectionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InfoFragment(private var song: Song = Song(SongInfoOptions.DUMMY_SONG_ID, "", "", "", "", 0, "", "", "")) : BottomSheetDialogFragment() {

    companion object {
        private const val SONG = "SONG"
    }

    private lateinit var viewModel: InfoViewModel
    private lateinit var songListViewModel: SongListViewModel
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = if (song.id == SongInfoOptions.DUMMY_SONG_ID) {
            ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(InfoOptionsSelectionViewModel::class.java)
        } else {
            ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(InfoDisplayViewModel::class.java)
        }
        songListViewModel = if (parentFragment != null) {
            ViewModelProvider(requireParentFragment(), (requireActivity() as PlayerActivity).viewModelFactory).get(SongListViewModel::class.java)
        } else {
            ViewModelProvider(requireActivity(), (requireActivity() as PlayerActivity).viewModelFactory).get(SongListViewModel::class.java)
        }
        viewModel.song = song
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songInfo.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songInfo.adapter = InfoAdapter()
        viewModel.songRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it)
        }
        viewModel.searchTerm.observe(viewLifecycleOwner) {
            if (it != null) {
                songListViewModel.isSearching.value = true
                songListViewModel.searchedText.value = it
                dismiss()
            }
        }
        viewModel.isPlaylistListDisplayed.observe(viewLifecycleOwner) {
            if (it) {
                SelectPlaylistFragment(song.id).show(childFragmentManager, null)
            }
        }
        val selectionViewModel = viewModel
        if (selectionViewModel is InfoOptionsSelectionViewModel) {
            selectionViewModel.shouldUpdateSongList.observe(viewLifecycleOwner) {
                if (it == true) {
                    songListViewModel.refreshQuickOptions()
                }
            }
        }
    }

    inner class InfoAdapter : ListAdapter<SongInfoOptions.SongRow, InfoViewHolder>(object : DiffUtil.ItemCallback<SongInfoOptions.SongRow>() {
        override fun areItemsTheSame(oldItem: SongInfoOptions.SongRow, newItem: SongInfoOptions.SongRow): Boolean {
            val isSameAction = (oldItem.actionType == newItem.actionType)
                    || (oldItem.actionType == SongInfoOptions.ActionType.EXPANDED_TITLE && newItem.actionType == SongInfoOptions.ActionType.EXPANDABLE_TITLE)
                    || (oldItem.actionType == SongInfoOptions.ActionType.EXPANDABLE_TITLE && newItem.actionType == SongInfoOptions.ActionType.EXPANDED_TITLE)
            return oldItem.fieldType == newItem.fieldType && isSameAction && oldItem.order == newItem.order
        }

        override fun areContentsTheSame(oldItem: SongInfoOptions.SongRow, newItem: SongInfoOptions.SongRow): Boolean = areItemsTheSame(oldItem, newItem) && oldItem.actionType == newItem.actionType
    }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder = InfoViewHolder(parent)

        override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onBindViewHolder(holder: InfoViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                if (payloads[0] == true) {
                    holder.bind(getItem(position))
                }
            }
        }

    }

    inner class InfoViewHolder(val parent: ViewGroup, val binding: ItemInfoBinding = ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: SongInfoOptions.SongRow) {
            binding.display = row
            binding.descriptionText = when {
                row.text == null && row.textRes != null -> parent.context.resources.getString(row.textRes)
                row.text != null && row.textRes == null -> row.text
                row.text != null && row.textRes != null -> parent.context.resources.getString(row.textRes, row.text)
                else -> ""
            }
            binding.root.setOnClickListener { viewModel.executeAction(row.fieldType, row.actionType) }
            if (row.actionType == SongInfoOptions.ActionType.NONE) {
                binding.root.setBackgroundColor(ResourcesCompat.getColor(parent.context.resources, R.color.primaryBackground, parent.context.theme))
            } else if (row.actionType != SongInfoOptions.ActionType.INFO_TITLE && row.actionType != SongInfoOptions.ActionType.EXPANDABLE_TITLE && row.actionType != SongInfoOptions.ActionType.EXPANDED_TITLE) {
                binding.root.setBackgroundColor(ResourcesCompat.getColor(parent.context.resources, R.color.accentBackground, parent.context.theme))
            } else {
                binding.root.setBackgroundColor(0)
            }
        }
    }
}
