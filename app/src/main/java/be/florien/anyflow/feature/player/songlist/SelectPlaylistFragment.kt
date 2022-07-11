package be.florien.anyflow.feature.player.songlist

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.databinding.FragmentSelectPlaylistBinding
import be.florien.anyflow.databinding.ItemSelectPlaylistBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.refreshVisibleViewHolders
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.UserScope
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@ActivityScope
@UserScope
class SelectPlaylistFragment(private var songId: Long = 0L) : DialogFragment() {
    lateinit var viewModel: SelectPlaylistViewModel
    private lateinit var fragmentBinding: FragmentSelectPlaylistBinding

    init {
        arguments?.let {
            songId = it.getLong("SongId")
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putLong("SongId", songId)
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
        fragmentBinding.songId = songId
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        fragmentBinding.filterList.adapter = FilterListAdapter(viewModel::isSelected, viewModel::toggleSelection)
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.filterList.addItemDecoration(
                    DividerItemDecoration(
                        requireActivity(),
                        DividerItemDecoration.VERTICAL
                    ).apply { setDrawable(it) })
            }
        viewModel.values.observe(viewLifecycleOwner) {
            if (it != null)
                (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(lifecycle, it)
        }
        viewModel.currentSelectionLive.observe(viewLifecycleOwner) {
            fragmentBinding.filterList.refreshVisibleViewHolders { vh ->
                val songViewHolder = vh as? PlaylistViewHolder
                songViewHolder?.setSelection(it.contains(songViewHolder.getCurrentId()))
            }
        }
        viewModel.isCreating.observe(viewLifecycleOwner) {
            if (it) {
                val editText = EditText(requireActivity())
                editText.inputType =
                    EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                AlertDialog.Builder(requireActivity())
                    .setView(editText)
                    .setTitle(R.string.info_action_new_playlist)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        viewModel.createPlaylist(editText.text.toString())
                    }
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
                        dialog.cancel()
                    }
                    .show()
            }
        }
        viewModel.isFinished.observe(viewLifecycleOwner) {
            if (it) {
                dismiss()
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

    class FilterListAdapter(override val isSelected: (Long) -> Boolean,
                            override val setSelected: (Long) -> Unit
    ) :
        PagingDataAdapter<Playlist, PlaylistViewHolder>(object :
            DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean =
                oldItem.id == newItem.id && oldItem.name == newItem.name && oldItem.count == newItem.count

        }), FastScrollRecyclerView.SectionedAdapter, BaseSelectableAdapter<Long> {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder =
            PlaylistViewHolder(parent, setSelected)

        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            holder.bind(filter, isSelected(filter.id))
        }

        override fun getSectionName(position: Int): String =
            snapshot()[position]?.name?.firstOrNull()?.uppercaseChar()?.toString()
                ?: ""
    }

    class PlaylistViewHolder(
        parent: ViewGroup,
        override val onSelectChange: (Long) -> Unit,
        private val itemPlaylistBinding: ItemSelectPlaylistBinding
        = ItemSelectPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) : RecyclerView.ViewHolder(itemPlaylistBinding.root), BaseSelectableAdapter.BaseSelectableViewHolder<Long, Playlist> {

        init {
            itemPlaylistBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }

        override fun bind(item: Playlist, isSelected: Boolean) {
            itemPlaylistBinding.item = item
            setSelection(isSelected)
            itemPlaylistBinding.root.setOnClickListener {
                onSelectChange(item.id)
            }
        }

        override fun setSelection(isSelected: Boolean) {
            itemPlaylistBinding.selected = isSelected
        }

        override fun getCurrentId(): Long? = itemPlaylistBinding.item?.id
    }
}
