package be.florien.anyflow.feature.podcast.base.ui

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
import be.florien.anyflow.feature.podcast.base.domain.BasePodcastInfoActions
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.podcast.base.ui.databinding.FragmentPodcastInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BasePodcastInfoFragment<T : BasePodcastViewModel>(
    private var podcastId: Long = BasePodcastInfoActions.DUMMY_PODCAST_ID
) : BottomSheetDialogFragment() {

    abstract fun getPodcastViewModel(): T

    companion object {
        private const val SONG = "SONG"
        private const val TOP_PADDING = 200
    }

    protected lateinit var viewModel: T
    private lateinit var binding: FragmentPodcastInfoBinding

    init {
        arguments?.let {
            podcastId = it.getLong(SONG, podcastId)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putLong(SONG, podcastId)
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
        viewModel = getPodcastViewModel()
        viewModel.podcastId = podcastId
        viewModel.podcastInfoObservable.observe(this) {
            viewModel.updateRows()
        }
        binding = FragmentPodcastInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.podcastInfo.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.podcastInfo.adapter = InfoAdapter(::executeAction)
        binding.cover.setOnClickListener {
            val imageConfig = viewModel.coverConfig.value
            if (imageConfig != null) {
                displayImageFullScreen(requireContext(), imageConfig)
            }
        }
        viewModel.infoRows.observe(viewLifecycleOwner) {
            val infoAdapter = binding.podcastInfo.adapter as InfoAdapter
            infoAdapter.submitList(it.map { infoRow -> infoRow.toInfoRow() })
        }
    }

    abstract fun BasePodcastInfoRow.toInfoRow() : InfoRow

    private fun executeAction(row: InfoRow) {
        val tag = row.tag
        if (tag is BasePodcastInfoRow) {
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
