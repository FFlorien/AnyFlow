package be.florien.anyflow.feature.song.base.ui

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import be.florien.anyflow.component.image.display.displayImageFullScreen
import be.florien.anyflow.component.info.InfoAdapter
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.ui.databinding.FragmentInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BaseSongInfoFragment<T : BaseSongViewModel>(
    private var songId: Long = BaseSongInfoActions.DUMMY_SONG_ID
) : BottomSheetDialogFragment() {

    abstract fun getSongViewModel(): T

    companion object {
        private const val SONG = "SONG"

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
        binding.songInfo.adapter = InfoAdapter(::executeAction)
        binding.cover.setOnClickListener {
            val imageConfig = viewModel.coverConfig.value
            if (imageConfig != null) {
                displayImageFullScreen(requireContext(), imageConfig)
            }
        }
        viewModel.infoRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.songInfo.adapter as InfoAdapter
            infoAdapter.submitList(it.map { infoRow -> infoRow.toInfoRow() })
        }
    }

    abstract fun BaseSongInfoRow.toInfoRow() : InfoRow

    private fun executeAction(row: InfoRow) {
        val tag = row.tag
        if (tag is BaseSongInfoRow) {
            viewModel.executeAction(tag)
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
}
