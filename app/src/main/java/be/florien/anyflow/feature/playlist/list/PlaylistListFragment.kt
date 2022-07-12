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
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.implementation.DeletePlaylistMenuHolder
import be.florien.anyflow.feature.menu.implementation.NewPlaylistMenuHolder
import be.florien.anyflow.feature.playlist.deletePlaylistConfirmation
import be.florien.anyflow.feature.playlist.newPlaylist
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsFragment
import be.florien.anyflow.feature.refreshVisibleViewHolders
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class PlaylistListFragment : BaseFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: PlaylistListViewModel
    private val menuCoordinator = MenuCoordinator()
    private val newPlaylistMenuHolder = NewPlaylistMenuHolder{
        requireActivity().newPlaylist(viewModel)
    }
    private val deletePlaylistMenuHolder = DeletePlaylistMenuHolder{
        requireActivity().deletePlaylistConfirmation(viewModel)
    }

    override fun getTitle() = resources.getString(R.string.menu_playlist)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity()
        menuCoordinator.addMenuHolder(newPlaylistMenuHolder)
        menuCoordinator.addMenuHolder(deletePlaylistMenuHolder)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[PlaylistListViewModel::class.java]
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
        recyclerView.adapter = PlaylistAdapter(this::onItemClicked, viewModel::isSelected, viewModel::toggleSelection)
        viewModel.playlistList.observe(viewLifecycleOwner) {
            (recyclerView.adapter as PlaylistAdapter).submitData(lifecycle, it)
        }
        viewModel.selection.observe(viewLifecycleOwner) {
            recyclerView.refreshVisibleViewHolders { vh ->
                val viewHolder = vh as PlaylistViewHolder
                viewHolder.setSelection(it.contains(vh.getCurrentId()))
            }
        }
        viewModel.hasSelection.observe(viewLifecycleOwner) {
            newPlaylistMenuHolder.isVisible = !it
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
        menuCoordinator.removeMenuHolder(deletePlaylistMenuHolder)
    }

    private fun onItemClicked(item: Playlist?) {
        if (viewModel.hasSelection.value == true) {
            item?.id?.let { viewModel.toggleSelection(it) }
        } else {
            if (item != null)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, PlaylistSongsFragment(item)).addToBackStack(null)
                    .commit()
        }
    }

    class PlaylistAdapter(
        private val onItemClickListener: (Playlist?) -> Unit,
        override val isSelected: (Long) -> Boolean,
        override val setSelected: (Long) -> Unit
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
        }), FastScrollRecyclerView.SectionedAdapter, BaseSelectableAdapter<Long> {
        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            val item = getItem(position) ?: return
            holder.bind(item, isSelected(item.id))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistViewHolder(parent, onItemClickListener, setSelected)

        override fun getSectionName(position: Int): String = getItem(position)?.name?.firstOrNull()?.toString() ?: position.toString()

    }

    class PlaylistViewHolder(
        container: ViewGroup,
        val onItemClickListener: (Playlist?) -> Unit,
        override val onSelectChange: (Long) -> Unit,
        private val binding: ItemPlaylistBinding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root), BaseSelectableAdapter.BaseSelectableViewHolder<Long, Playlist> {

        init {
            binding.lifecycleOwner = container.findViewTreeLifecycleOwner()
            binding.root.setOnClickListener {
                onItemClickListener(binding.item)
            }
            binding.root.setOnLongClickListener {
                val item = binding.item ?: return@setOnLongClickListener false
                onSelectChange(item.id)
                true
            }
        }

        override fun bind(item: Playlist, isSelected: Boolean) {
            binding.item = item
        }

        override fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        override fun getCurrentId(): Long? = binding.item?.id
    }
}