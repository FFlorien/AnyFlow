package be.florien.anyflow.feature.playlist.songs

import android.content.Context
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
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.list.BaseSelectableAdapter
import be.florien.anyflow.common.ui.list.refreshVisibleViewHolders
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.playlist.PlaylistsActivity
import be.florien.anyflow.feature.playlist.menu.PlayPlaylistSongsMenuHolder
import be.florien.anyflow.feature.playlist.menu.RemoveSongsMenuHolder
import be.florien.anyflow.feature.playlist.ui.databinding.ItemPlaylistSongBinding
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.management.playlist.model.PlaylistSong
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.feature.playlist.ui.R as ModuleR
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import javax.inject.Inject

class PlaylistSongsFragment(private var playlist: PlaylistWithCount? = null) : BaseFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var viewModel: PlaylistSongsViewModel
    private val menuCoordinator = MenuCoordinator()
    private val playPlaylistMenuHolder = PlayPlaylistSongsMenuHolder {
        viewModel.filterOnPlaylist()
        navigator.navigateToMain(requireContext(), true)
    }
    private val removeFromPlaylistMenuHolder = RemoveSongsMenuHolder {
        requireActivity().removeSongsConfirmation(viewModel)
    }

    @Inject
    lateinit var navigator: Navigator

    init {
        arguments?.let { args ->
            (args.getParcelable(PLAYLIST_SONGS_PLAYLIST) as PlaylistWithCount?)?.let { playlist = it }
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
        (requireActivity() as PlaylistsActivity).component?.apply {
            inject(this@PlaylistSongsFragment)
            inject(viewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(ModuleR.layout.fragment_playlist_songs, container, false)

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
        PagingDataAdapter<PlaylistSong, PlaylistSongViewHolder>(object :
            DiffUtil.ItemCallback<PlaylistSong>() {
            override fun areItemsTheSame(
                oldItem: PlaylistSong,
                newItem: PlaylistSong
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PlaylistSong,
                newItem: PlaylistSong
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
        private val binding: ItemPlaylistSongBinding = ItemPlaylistSongBinding.inflate(
            //todo this is a duplicate of layoutSong from songlist, should do better to avoid this (put songlayout in common ?)
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root),
        BaseSelectableAdapter.BaseSelectableViewHolder<Long, PlaylistSong> {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
            binding.root.setOnClickListener {
                binding.song?.let { song -> onSelectChange(song.id) }
            }
        }

        override fun bind(item: PlaylistSong, isSelected: Boolean) {
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