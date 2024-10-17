package be.florien.anyflow.feature.song.base.ui

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import be.florien.anyflow.common.ui.component.ImageDisplayFragment
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoAdapter
import be.florien.anyflow.common.ui.info.InfoViewHolder
import be.florien.anyflow.feature.song.base.ui.databinding.FragmentInfoBinding
import be.florien.anyflow.feature.song.base.ui.databinding.ItemDownloadInfoBinding
import be.florien.anyflow.feature.song.base.ui.databinding.ItemShortcutInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BaseSongInfoFragment<IA : BaseSongInfoActions, T : BaseSongViewModel<IA>>(
    private var songId: Long = BaseSongInfoActions.DUMMY_SONG_ID
) : BottomSheetDialogFragment() {

    abstract fun getSongViewModel(): T

    companion object {
        private const val SONG = "SONG"

        private const val ITEM_VIEW_TYPE_DEFAULT = 0
        private const val ITEM_VIEW_TYPE_SHORTCUT = 1
        private const val ITEM_VIEW_TYPE_DOWNLOAD = 2
        private const val TOP_PADDING = 200
    }

    protected lateinit var viewModel: T

    //    private var songListViewModel: SongListViewModel? = null
    private lateinit var binding: FragmentInfoBinding

    init {
        arguments?.let {
            songId = it.getLong(SONG, songId)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putLong(SONG, songId)
            }

        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetDialogAnimation
        dialog.setOnShowListener {
            setupFullHeight()
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = getSongViewModel()
        viewModel.songId = songId
        viewModel.songInfoObservable.observe(this) {
            viewModel.updateRows()
        }
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songInfo.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songInfo.adapter = SongInfoAdapter(viewModel::executeAction)
        binding.cover.setOnClickListener {
            ImageDisplayFragment(
                viewModel.coverConfig.value?.url ?: ""
            ).show(childFragmentManager, null)
        }
        viewModel.infoRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it)
        }
    }

    private fun setupFullHeight() {
        val layoutParams = binding.root.layoutParams
        val height = getWindowHeight() - TOP_PADDING
        if (layoutParams != null) {
            layoutParams.height = height
        }
        binding.root.layoutParams = layoutParams
    }

    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity?)!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    class SongInfoAdapter(private val executeAction: (row: InfoActions.InfoRow) -> Unit) :
        InfoAdapter<InfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            return when (viewType) {
                ITEM_VIEW_TYPE_SHORTCUT -> ShortcutInfoViewHolder(parent, executeAction)
                ITEM_VIEW_TYPE_DOWNLOAD -> DownloadInfoViewHolder(parent, executeAction)
                else -> InfoViewHolder(parent, executeAction)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is BaseSongInfoActions.ShortcutInfoRow -> ITEM_VIEW_TYPE_SHORTCUT
                is BaseSongInfoActions.SongDownload -> ITEM_VIEW_TYPE_DOWNLOAD
                else -> ITEM_VIEW_TYPE_DEFAULT
            }
        }
    }

    class ShortcutInfoViewHolder(
        parent: ViewGroup,
        executeAction: (row: InfoActions.InfoRow) -> Unit,
        private val parentBinding: ItemShortcutInfoBinding = ItemShortcutInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root) {

        override fun setLifecycleOwner() {
            parentBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }
    }

    class DownloadInfoViewHolder(
        parent: ViewGroup,
        executeAction: (row: InfoActions.InfoRow) -> Unit,
        private val parentBinding: ItemDownloadInfoBinding = ItemDownloadInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root) {

        override fun bindChangedData(row: InfoActions.InfoRow) {
            super.bindChangedData(row)
            if (row is BaseSongInfoActions.SongDownload) {
                parent.findViewTreeLifecycleOwner()?.let {
                    row.progress.observe(it) { progress ->
                        parentBinding.progress.max = progress.total
                        parentBinding.progress.progress = progress.downloaded
                        parentBinding.progress.secondaryProgress =
                            progress.downloaded + progress.queued
                    }
                }
            }
        }

        override fun setLifecycleOwner() {
            parentBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }
    }
}
