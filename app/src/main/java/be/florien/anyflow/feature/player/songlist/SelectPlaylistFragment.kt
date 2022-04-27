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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectPlaylistBinding
import be.florien.anyflow.databinding.ItemSelectPlaylistBinding
import be.florien.anyflow.extension.viewModelFactory
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory).get(SelectPlaylistViewModel::class.java) //todo change get viewmodel from attach to oncreateView in other fragments
        fragmentBinding = FragmentSelectPlaylistBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.songId = songId
        fragmentBinding.filterList.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        fragmentBinding.filterList.adapter = FilterListAdapter()
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
        }
        viewModel.values.observe(viewLifecycleOwner) {
            if (it != null)
                (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(lifecycle, it)
        }
        viewModel.currentSelectionLive.observe(viewLifecycleOwner) {
            (fragmentBinding.filterList.adapter as FilterListAdapter).notifyDataSetChanged()
        }
        viewModel.isCreating.observe(viewLifecycleOwner) {
            if (it) {
                val editText = EditText(requireActivity())
                editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
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
            parentFragment.onDismiss(dialog);
        }
        super.onDismiss(dialog)
    }

    inner class FilterListAdapter : PagingDataAdapter<SelectPlaylistViewModel.SelectionItem, PlaylistViewHolder>(object : DiffUtil.ItemCallback<SelectPlaylistViewModel.SelectionItem>() {
        override fun areItemsTheSame(oldItem: SelectPlaylistViewModel.SelectionItem, newItem: SelectPlaylistViewModel.SelectionItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SelectPlaylistViewModel.SelectionItem, newItem: SelectPlaylistViewModel.SelectionItem): Boolean =
            oldItem.displayName == newItem.displayName && oldItem.isSelected == (viewModel.currentSelectionLive.value?.any { it.id == newItem.id })

    }), FastScrollRecyclerView.SectionedAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder = PlaylistViewHolder()

        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            filter.isSelected = viewModel.currentSelectionLive.value?.any { it.id == filter.id } ?: false
            holder.bind(filter)
        }

        override fun getSectionName(position: Int): String =
            snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                ?: ""
    }

    inner class PlaylistViewHolder(
        private val itemPlaylistBinding: ItemSelectPlaylistBinding
        = ItemSelectPlaylistBinding.inflate(layoutInflater, fragmentBinding.filterList, false)
    ) : RecyclerView.ViewHolder(itemPlaylistBinding.root) {

        fun bind(selection: SelectPlaylistViewModel.SelectionItem) {
            itemPlaylistBinding.vm = viewModel
            itemPlaylistBinding.lifecycleOwner = viewLifecycleOwner
            itemPlaylistBinding.item = selection
            setBackground(itemPlaylistBinding.root, selection)
            itemPlaylistBinding.root.setOnClickListener {
                viewModel.changeFilterSelection(selection)
                setBackground(itemPlaylistBinding.root, selection)
            }
        }

        private fun setBackground(view: View, selection: SelectPlaylistViewModel.SelectionItem?) {
            view.setBackgroundColor(ResourcesCompat.getColor(resources, if (selection?.isSelected == true) R.color.selected else R.color.unselected, requireActivity().theme))
        }
    }
}
