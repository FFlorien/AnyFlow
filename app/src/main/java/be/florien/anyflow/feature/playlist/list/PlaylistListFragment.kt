package be.florien.anyflow.feature.playlist.list

import android.content.Context
import android.os.Bundle
import android.view.*
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
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.implementation.DeletePlaylistMenuHolder
import be.florien.anyflow.feature.menu.implementation.NewPlaylistMenuHolder
import be.florien.anyflow.feature.menu.implementation.PlayPlaylistMenuHolder
import be.florien.anyflow.feature.menu.implementation.SelectionModeMenuHolder
import be.florien.anyflow.feature.playlist.deletePlaylistConfirmation
import be.florien.anyflow.feature.playlist.newPlaylist
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsFragment
import be.florien.anyflow.feature.refreshVisibleViewHolders
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class PlaylistListFragment : BaseFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: PlaylistListViewModel
    private val menuCoordinator = MenuCoordinator()
    private val newPlaylistMenuHolder = NewPlaylistMenuHolder {
        requireActivity().newPlaylist(viewModel)
    }
    private val playPlaylistMenuHolder = PlayPlaylistMenuHolder {
        viewModel.filterOnSelection()
        requireActivity().finish()
    }
    private val deletePlaylistMenuHolder = DeletePlaylistMenuHolder {
        requireActivity().deletePlaylistConfirmation(viewModel)
    }
    private val selectPlaylistMenuHolder by lazy {
        SelectionModeMenuHolder(viewModel.hasSelection.value == true, requireContext()) {
            viewModel.toggleSelectionMode()
        }
    }

    override fun getTitle() = resources.getString(R.string.menu_playlist)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(newPlaylistMenuHolder)
        menuCoordinator.addMenuHolder(playPlaylistMenuHolder)
        menuCoordinator.addMenuHolder(deletePlaylistMenuHolder)
        menuCoordinator.addMenuHolder(selectPlaylistMenuHolder)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[PlaylistListViewModel::class.java]
        anyFlowApp.serverComponent?.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter =
            PlaylistAdapter(this::onItemClicked, viewModel::isSelected, viewModel::toggleSelection)
        viewModel.playlistList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistAdapter).submitData(lifecycle, it)
        }
        viewModel.selection.observe(viewLifecycleOwner) {
            recyclerView.refreshVisibleViewHolders { vh ->
                val viewHolder = vh as PlaylistViewHolder
                viewHolder.setSelection(it.contains(vh.getCurrentId()))
            }
        }
        viewModel.isInSelectionMode.observe(viewLifecycleOwner) {
            newPlaylistMenuHolder.isVisible = !it
            selectPlaylistMenuHolder.changeState(!it)
        }
        viewModel.hasSelection.observe(viewLifecycleOwner) {
            playPlaylistMenuHolder.isVisible = it
            deletePlaylistMenuHolder.isVisible = it
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
        menuCoordinator.removeMenuHolder(newPlaylistMenuHolder)
        menuCoordinator.removeMenuHolder(playPlaylistMenuHolder)
        menuCoordinator.removeMenuHolder(deletePlaylistMenuHolder)
        menuCoordinator.removeMenuHolder(selectPlaylistMenuHolder)
    }

    private fun onItemClicked(item: Playlist?) {
        if (viewModel.isInSelectionMode.value == true) {
            item?.let { viewModel.toggleSelection(it) }
        } else {
            if (item != null)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, PlaylistSongsFragment(item)).addToBackStack(null)
                    .commit()
        }
    }

    class PlaylistAdapter(
        private val onItemClickListener: (Playlist?) -> Unit,
        override val isSelected: (Playlist) -> Boolean,
        override val setSelected: (Playlist) -> Unit
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
        }), FastScrollRecyclerView.SectionedAdapter, BaseSelectableAdapter<Playlist> {
        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            val item = getItem(position) ?: return
            holder.bind(item, isSelected(item))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistViewHolder(parent, onItemClickListener, setSelected)

        override fun getSectionName(position: Int): String =
            getItem(position)?.name?.firstOrNull()?.toString() ?: position.toString()

    }

    class PlaylistViewHolder(
        container: ViewGroup,
        val onItemClickListener: (Playlist?) -> Unit,
        override val onSelectChange: (Playlist) -> Unit,
        private val binding: ItemPlaylistBinding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root),
        BaseSelectableAdapter.BaseSelectableViewHolder<Playlist, Playlist> {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
            binding.root.setOnClickListener {
                onItemClickListener(binding.item)
            }
            binding.root.setOnLongClickListener {
                val item = binding.item ?: return@setOnLongClickListener false
                onSelectChange(item)
                true
            }
        }

        override fun bind(item: Playlist, isSelected: Boolean) {
            binding.item = item
        }

        override fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        override fun getCurrentId(): Playlist? = binding.item
    }
}