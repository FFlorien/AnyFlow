package be.florien.anyflow.feature.playlist.list

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
import be.florien.anyflow.databinding.ItemPlaylistBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsFragment

class PlaylistListFragment : BaseFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var viewModel: PlaylistListViewModel

    override fun getTitle() = resources.getString(R.string.menu_playlist)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(PlaylistListViewModel::class.java)
        anyFlowApp.applicationComponent.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PlaylistAdapter(this::onItemClicked)
        viewModel.playlistList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistAdapter).submitData(lifecycle, it)
        }
    }

    private fun onItemClicked(item: Playlist?) {
        if (item != null)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, PlaylistSongsFragment(item)).addToBackStack(null).commit()
    }

    class PlaylistAdapter(
        private val onItemClickListener: (Playlist?) -> Unit
    ) :
        PagingDataAdapter<Playlist, PlaylistViewHolder>(object : DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(
                oldItem: Playlist,
                newItem: Playlist
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: Playlist,
                newItem: Playlist
            ) = areItemsTheSame(oldItem, newItem) && oldItem.name == newItem.name
        }) {
        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistViewHolder(parent, onItemClickListener)
    }

    class PlaylistViewHolder(
        container: ViewGroup,
        val onItemClickListener: (Playlist?) -> Unit,
        private val binding: ItemPlaylistBinding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
        }

        fun bind(item: Playlist?) {
            binding.item = item
            binding.root.setOnClickListener {
                onItemClickListener(item)
            }
        }
    }
}