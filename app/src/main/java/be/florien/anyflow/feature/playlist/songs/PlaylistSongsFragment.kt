package be.florien.anyflow.feature.playlist.songs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.databinding.LayoutSongBinding
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.implementation.PlayPlaylistSongsMenuHolder
import be.florien.anyflow.feature.menu.implementation.RemoveSongsMenuHolder
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.feature.refreshVisibleViewHolders
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class PlaylistSongsFragment(private var playlist: Playlist? = null) : BaseFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var viewModel: PlaylistSongsViewModel
    private val menuCoordinator = MenuCoordinator()
    private val playPlaylistMenuHolder = PlayPlaylistSongsMenuHolder {
        viewModel.filterOnPlaylist()
        val intent = Intent(requireContext(), PlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        requireActivity().startActivity(intent)
    }
    private val removeFromPlaylistMenuHolder = RemoveSongsMenuHolder {
        requireActivity().removeSongsConfirmation(viewModel)
    }

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

    override fun getTitle() =
        playlist?.name ?: throw IllegalStateException("Playlist shouldn't be null !")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(playPlaylistMenuHolder)
        menuCoordinator.addMenuHolder(removeFromPlaylistMenuHolder)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[PlaylistSongsViewModel::class.java]
        viewModel.playlist = playlist ?: throw IllegalStateException("Playlist shouldn't be null !")
        anyFlowApp.serverComponent?.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist_songs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PlaylistSongAdapter(
            viewModel::getCover,
            viewModel::isSelected,
            viewModel::toggleSelection
        )
        viewModel.songList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistSongAdapter).submitData(lifecycle, it)
        }
        viewModel.selectionList.observe(viewLifecycleOwner) {
            recyclerView.refreshVisibleViewHolders { vh ->
                val songViewHolder = vh as? PlaylistSongViewHolder
                songViewHolder?.setSelection(it.contains(songViewHolder.getCurrentId()))
            }
        }
        viewModel.hasSelection.observe(viewLifecycleOwner) {
            removeFromPlaylistMenuHolder.isVisible = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuCoordinator.inflateMenus(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menuCoordinator.prepareMenus(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(playPlaylistMenuHolder)
        menuCoordinator.removeMenuHolder(removeFromPlaylistMenuHolder)
    }

    class PlaylistSongAdapter(
        private val getCoverUrl: (Long) -> String,
        override val isSelected: (Long) -> Boolean,
        override val setSelected: (Long) -> Unit
    ) :
        PagingDataAdapter<SongDisplay, PlaylistSongViewHolder>(object :
            DiffUtil.ItemCallback<SongDisplay>() {
            override fun areItemsTheSame(
                oldItem: SongDisplay,
                newItem: SongDisplay
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SongDisplay,
                newItem: SongDisplay
            ) = areItemsTheSame(oldItem, newItem)
        }), FastScrollRecyclerView.SectionedAdapter, BaseSelectableAdapter<Long> {

        override fun onBindViewHolder(holder: PlaylistSongViewHolder, position: Int) {
            val item = getItem(position) ?: return
            holder.bind(item, isSelected(item.id))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistSongViewHolder(parent, getCoverUrl, setSelected)

        override fun getSectionName(position: Int) = position.plus(1).toString()
    }

    class PlaylistSongViewHolder(
        container: ViewGroup,
        private val getCoverUrl: (Long) -> String,
        override val onSelectChange: (Long) -> Unit,
        private val binding: LayoutSongBinding = LayoutSongBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root),
        BaseSelectableAdapter.BaseSelectableViewHolder<Long, SongDisplay> {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
            binding.root.setOnClickListener {
                binding.song?.let { song -> onSelectChange(song.id) }
            }
        }

        override fun bind(item: SongDisplay, isSelected: Boolean) {
            binding.song = item
            binding.art = ImageConfig(getCoverUrl(item.albumId), R.drawable.cover_placeholder)
            setSelection(isSelected)
        }

        override fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        override fun getCurrentId() = binding.song?.id
    }

    companion object {
        private const val PLAYLIST_SONGS_PLAYLIST = "PLAYLIST_SONGS_PLAYLIST"
    }
}