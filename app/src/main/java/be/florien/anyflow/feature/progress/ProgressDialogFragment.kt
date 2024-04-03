package be.florien.anyflow.feature.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentProgressBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TITLE = "title"

class ProgressDialog(
    private var title: String
) : DialogFragment() {

    private var binding: FragmentProgressBinding? = null

    init {
        val args = arguments
        if (args == null) {
            arguments = Bundle().apply {
                putString(TITLE, title)
            }
        } else {
            title = args.getString(TITLE) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.let {
            it.setBackgroundDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_primary_bg_radius,
                    null
                )
            )
            it.requestFeature(STYLE_NO_TITLE)
        }
        val inflate = FragmentProgressBinding.inflate(inflater, container, false)
        binding = inflate
        inflate.title = title
        inflate.isRunning = true
        return inflate.root
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        isCancelable = false
    }

    fun updateProgress(progress: Progress) {
        binding?.progress = progress
    }

    fun finish(onFinishEnd: (() -> Unit)) {
        binding?.isRunning = false
        lifecycleScope.launch {
            delay(1000)
            dismiss()
            onFinishEnd()
        }
    }

    data class Progress(
        val progress: Int,
        val total: Int
    )
}
