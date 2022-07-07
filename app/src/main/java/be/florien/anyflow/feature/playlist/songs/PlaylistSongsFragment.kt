package be.florien.anyflow.feature.playlist.songs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.LayoutSongBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class PlaylistSongsFragment(private var playlist: Playlist) : BaseFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var viewModel: PlaylistSongsViewModel

    init {
        arguments?.let { args ->
            (args.getParcelable(PLAYLIST_SONGS_PLAYLIST) as Playlist?)?.let { playlist = it }
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putParcelable(PLAYLIST_SONGS_PLAYLIST, playlist)
            }
        }
    }

    override fun getTitle() = playlist.name

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[PlaylistSongsViewModel::class.java]
        viewModel.playlistId = playlist.id
        anyFlowApp.applicationComponent.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist_songs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PlaylistSongAdapter(viewModel::getCover, viewModel::isSelected, viewModel::setSelection)
        viewModel.songList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistSongAdapter).submitData(lifecycle, it)
        }
        viewModel.selectionList.observe(viewLifecycleOwner) {
            val firstPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val lastPosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            for (position in firstPosition..lastPosition) {
                val songViewHolder =
                    recyclerView.findViewHolderForAdapterPosition(position) as? PlaylistSongViewHolder
                songViewHolder?.setSelection(it.contains(songViewHolder.getCurrentId()))
            }
        }
    }

    class PlaylistSongAdapter(
        private val getCoverUrl: (Long) -> String,
        private val isSelected: (Long?) -> Boolean,
        private val setSelected: (Long?, Boolean) -> Unit
    ) :
        PagingDataAdapter<SongInfo, PlaylistSongViewHolder>(object : DiffUtil.ItemCallback<SongInfo>() {
            override fun areItemsTheSame(
                oldItem: SongInfo,
                newItem: SongInfo
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SongInfo,
                newItem: SongInfo
            ) = areItemsTheSame(oldItem, newItem)
        }), FastScrollRecyclerView.SectionedAdapter {

        override fun onBindViewHolder(holder: PlaylistSongViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item, isSelected(item?.id))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistSongViewHolder(parent, getCoverUrl, setSelected)

        override fun getSectionName(position: Int) = position.plus(1).toString()
    }

    class PlaylistSongViewHolder(
        container: ViewGroup,
        private val getCoverUrl: (Long) -> String,
        private val onSelectChange: (Long, Boolean) -> Unit,
        private val binding: LayoutSongBinding = LayoutSongBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
            binding.root.setOnClickListener {
                binding.song?.let { song -> onSelectChange(song.id, !binding.selected) }
            }
        }

        fun bind(item: SongInfo?, isSelected: Boolean) {
            binding.song = item
            binding.art = item?.albumId?.let { getCoverUrl(it) }
            setSelection(isSelected)
        }

        fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        fun getCurrentId() = binding.song?.id
    }

    companion object {
        private const val PLAYLIST_SONGS_PLAYLIST = "PLAYLIST_SONGS_PLAYLIST"
    }
}