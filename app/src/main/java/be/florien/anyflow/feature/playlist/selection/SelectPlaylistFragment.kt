package be.florien.anyflow.feature.playlist.selection

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.databinding.FragmentSelectPlaylistBinding
import be.florien.anyflow.databinding.ItemSelectPlaylistBinding
import be.florien.anyflow.feature.info.progress.ProgressDialog
import be.florien.anyflow.feature.info.progress.ProgressDialog.Progress
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions.SongFieldType
import be.florien.anyflow.feature.playlist.newPlaylist
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ActivityScope
@be.florien.anyflow.architecture.di.ServerScope
class SelectPlaylistFragment(
    private var id: Long = 0L,
    private var type: SongFieldType = SongFieldType.Title,
    private var secondId: Int = -1
) : BottomSheetDialogFragment() {
    lateinit var viewModel: SelectPlaylistViewModel
    private lateinit var fragmentBinding: FragmentSelectPlaylistBinding
    private var progressDialog: ProgressDialog? = null

    init {
        arguments?.let {
            id = it.getLong("id")
            type = SongFieldType.valueOf(it.getString("type") ?: SongFieldType.Title.name)
            secondId = it.getInt("secondId")
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putLong("id", id)
                putString("type", type.name)
                putInt("secondId", secondId)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[SelectPlaylistViewModel::class.java] //todo change get viewmodel from attach to oncreateView in other fragments
        fragmentBinding = FragmentSelectPlaylistBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.playlistList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        fragmentBinding.playlistList.adapter = FilterListAdapter(viewModel::rotateActionForPlaylist)
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.playlistList.addItemDecoration(
                    DividerItemDecoration(
                        requireActivity(),
                        DividerItemDecoration.VERTICAL
                    ).apply { setDrawable(it) })
            }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.initViewModel(id, type, secondId)
            viewModel.values.observe(viewLifecycleOwner) {
                if (it != null)
                    (fragmentBinding.playlistList.adapter as FilterListAdapter).submitList(it)
            }
            viewModel.filterCount.observe(viewLifecycleOwner) {
                if (it != null)
                    (fragmentBinding.playlistList.adapter as FilterListAdapter).total = it
            }
            viewModel.isCreating.observe(viewLifecycleOwner) {
                if (it) {
                    requireActivity().newPlaylist(viewModel)
                }
            }
            viewModel.progressLiveData.observe(viewLifecycleOwner) { progressState ->
                if (progressState == null) {
                    return@observe
                }

                if (progressDialog == null) {
                    progressDialog = ProgressDialog(getString(R.string.playlist_progress_title))
                    progressDialog?.show(childFragmentManager, "progress")
                }
                when (progressState) {
                    is SelectPlaylistViewModel.ModificationProgress.InModificationProgress ->
                        progressDialog?.updateProgress(progressState.toProgressRunning())

                    SelectPlaylistViewModel.ModificationProgress.Finished ->
                        progressDialog?.finish { dismiss() }

                    SelectPlaylistViewModel.ModificationProgress.Cancelled ->
                        dismiss()
                }
            }

        }
        return fragmentBinding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        val parentFragment = parentFragment
        if (parentFragment is DialogInterface.OnDismissListener) {
            parentFragment.onDismiss(dialog)
        }
        super.onDismiss(dialog)
    }

    class FilterListAdapter(
        private val rotateAction: (Long) -> Unit
    ) : ListAdapter<SelectPlaylistViewModel.PlaylistWithAction, PlaylistViewHolder>(object :
        DiffUtil.ItemCallback<SelectPlaylistViewModel.PlaylistWithAction>() {
        override fun areItemsTheSame(
            oldItem: SelectPlaylistViewModel.PlaylistWithAction,
            newItem: SelectPlaylistViewModel.PlaylistWithAction
        ) =
            oldItem.playlist.id == newItem.playlist.id

        override fun areContentsTheSame(
            oldItem: SelectPlaylistViewModel.PlaylistWithAction,
            newItem: SelectPlaylistViewModel.PlaylistWithAction
        ): Boolean = oldItem.playlist.id == newItem.playlist.id
                && oldItem.playlist.name == newItem.playlist.name
                && oldItem.playlist.count == newItem.playlist.count
                && oldItem.action == newItem.action

    }), FastScrollRecyclerView.SectionedAdapter {

        var total: Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder =
            PlaylistViewHolder(parent, rotateAction)

        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            holder.bind(filter, total)
        }

        override fun getSectionName(position: Int): String =
            currentList[position]?.playlist?.name?.firstOrNull()?.uppercaseChar()?.toString()
                ?: ""
    }

    class PlaylistViewHolder(
        parent: ViewGroup,
        val onActionChange: (Long) -> Unit,
        private val itemPlaylistBinding: ItemSelectPlaylistBinding
        = ItemSelectPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) : RecyclerView.ViewHolder(itemPlaylistBinding.root) {

        init {
            itemPlaylistBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }

        fun bind(item: SelectPlaylistViewModel.PlaylistWithAction, total: Int) {
            itemPlaylistBinding.item = item.playlist
            itemPlaylistBinding.total = total
            itemPlaylistBinding.presenceBg = when (item.playlist.presence) {
                0 -> R.drawable.bg_corner_radius_none
                total -> R.drawable.bg_corner_radius_all
                else -> R.drawable.bg_corner_radius_partial
            }
            setAction(item.action)
            itemPlaylistBinding.root.setOnClickListener {
                onActionChange(item.playlist.id)
            }
        }

        private fun setAction(action: SelectPlaylistViewModel.PlaylistAction) {
            itemPlaylistBinding.isAdding = action == SelectPlaylistViewModel.PlaylistAction.ADDITION
            itemPlaylistBinding.isRemoving = action == SelectPlaylistViewModel.PlaylistAction.DELETION
        }
    }

    private fun SelectPlaylistViewModel.ModificationProgress.InModificationProgress.toProgressRunning() =
        Progress(playlistIndex, playlistCount)
}
