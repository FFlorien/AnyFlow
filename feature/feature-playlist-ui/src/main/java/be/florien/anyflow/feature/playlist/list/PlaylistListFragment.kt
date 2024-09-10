package be.florien.anyflow.feature.playlist.list

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
import be.florien.anyflow.common.ui.list.BaseSelectableAdapter
import be.florien.anyflow.common.ui.list.refreshVisibleViewHolders
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.feature.playlist.PlaylistsActivity
import be.florien.anyflow.common.ui.component.deletePlaylistConfirmation
import be.florien.anyflow.feature.playlist.menu.DeletePlaylistMenuHolder
import be.florien.anyflow.feature.playlist.menu.NewPlaylistMenuHolder
import be.florien.anyflow.feature.playlist.menu.PlayPlaylistMenuHolder
import be.florien.anyflow.feature.playlist.menu.SelectionModeMenuHolder
import be.florien.anyflow.common.ui.component.newPlaylist
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsFragment
import be.florien.anyflow.feature.playlist.ui.databinding.ItemPlaylistBinding
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.resources.R
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import be.florien.anyflow.feature.playlist.ui.R as ModuleR

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
        (requireActivity() as PlaylistsActivity).component?.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(ModuleR.layout.fragment_playlist_list, container, false)

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

    private fun onItemClicked(item: PlaylistWithCount?) {
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
        private val onItemClickListener: (PlaylistWithCount?) -> Unit,
        override val isSelected: (PlaylistWithCount) -> Boolean,
        override val setSelected: (PlaylistWithCount) -> Unit
    ) :
        PagingDataAdapter<PlaylistWithCount, PlaylistViewHolder>(object : DiffUtil.ItemCallback<PlaylistWithCount>() {
            override fun areItemsTheSame(
                oldItem: PlaylistWithCount,
                newItem: PlaylistWithCount
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PlaylistWithCount,
                newItem: PlaylistWithCount
            ) = areItemsTheSame(oldItem, newItem) && oldItem.name == newItem.name
        }), FastScrollRecyclerView.SectionedAdapter, BaseSelectableAdapter<PlaylistWithCount> {
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
        val onItemClickListener: (PlaylistWithCount?) -> Unit,
        override val onSelectChange: (PlaylistWithCount) -> Unit,
        private val binding: ItemPlaylistBinding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root),
        BaseSelectableAdapter.BaseSelectableViewHolder<PlaylistWithCount, PlaylistWithCount> {

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

        override fun bind(item: PlaylistWithCount, isSelected: Boolean) {
            binding.item = item
        }

        override fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        override fun getCurrentId(): PlaylistWithCount? = binding.item
    }
}