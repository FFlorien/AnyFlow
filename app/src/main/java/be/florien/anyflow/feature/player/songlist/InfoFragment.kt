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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InfoFragment(private var song: Song) : BottomSheetDialogFragment() {

    companion object {
        private const val SONG = "SONG"
    }

    private lateinit var viewModel: InfoViewModel
    private lateinit var songListViewModel: SongListViewModel
    private lateinit var binding: FragmentInfoBinding


    init {
        arguments?.let {
            song = it.getParcelable(SONG) ?: Song(0L, "", "", "", "", 0, "", "", "")
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putParcelable(SONG, song)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(InfoViewModel::class.java)
        songListViewModel = ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(SongListViewModel::class.java)
        viewModel.songId = song.id
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songInfo.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songInfo.adapter = InfoAdapter()
        viewModel.songRows.observe(viewLifecycleOwner, {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it)
        })
        viewModel.searchTerm.observe(viewLifecycleOwner) {
            if (it != null) {
                songListViewModel.isSearching.value = true
                songListViewModel.searchedText.value = it
                dismiss()
            }
        }
    }

    class InfoAdapter : ListAdapter<InfoViewModel.SongRow, InfoViewHolder>(object : DiffUtil.ItemCallback<InfoViewModel.SongRow>() {
        override fun areItemsTheSame(oldItem: InfoViewModel.SongRow, newItem: InfoViewModel.SongRow): Boolean {
            return oldItem.fieldType == newItem.fieldType && oldItem.actionType == newItem.actionType
        }

        override fun areContentsTheSame(oldItem: InfoViewModel.SongRow, newItem: InfoViewModel.SongRow): Boolean = areItemsTheSame(oldItem, newItem) && oldItem.icon == newItem.icon
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

    class InfoViewHolder(val parent: ViewGroup, val binding: ItemInfoBinding = ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: InfoViewModel.SongRow) {
            binding.display = row
            binding.descriptionText = when {
                row.text == null && row.textRes != null -> parent.context.resources.getString(row.textRes)
                row.text != null && row.textRes == null -> row.text
                row.text != null && row.textRes != null -> parent.context.resources.getString(row.textRes, row.text)
                else -> ""
            }
            binding.root.setOnClickListener { row.action?.invoke(row.fieldType) }
            if (row.actionType != InfoViewModel.ActionType.NONE && row.actionType != InfoViewModel.ActionType.EXPAND) {
                binding.root.setBackgroundColor(ResourcesCompat.getColor(parent.context.resources, R.color.accentLightPlus, parent.context.theme))
            } else {
                binding.root.setBackgroundColor(0)
            }
        }
    }
}
