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
import be.florien.anyflow.databinding.ItemSongBinding
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
        ).get(PlaylistSongsViewModel::class.java)
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
        recyclerView.adapter = PlaylistAdapter(viewModel::getCover)
        viewModel.songList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistAdapter).submitData(lifecycle, it)
        }
    }

    class PlaylistAdapter(
        private val getCoverUrl: (Long) -> String
    ) :
        PagingDataAdapter<SongInfo, PlaylistViewHolder>(object : DiffUtil.ItemCallback<SongInfo>() {
            override fun areItemsTheSame(
                oldItem: SongInfo,
                newItem: SongInfo
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SongInfo,
                newItem: SongInfo
            ) = areItemsTheSame(oldItem, newItem)
        }), FastScrollRecyclerView.SectionedAdapter {
        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistViewHolder(parent, getCoverUrl)

        override fun getSectionName(position: Int) = position.plus(1).toString()
    }

    class PlaylistViewHolder(
        container: ViewGroup,
        val getCoverUrl: (Long) -> String,
        val binding: ItemSongBinding = ItemSongBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
        }

        fun bind(item: SongInfo?) {
            binding.song = item
            binding.art = item?.albumId?.let { getCoverUrl(it) }
        }
    }

    companion object {
        private const val PLAYLIST_SONGS_PLAYLIST = "PLAYLIST_SONGS_PLAYLIST"
    }
}