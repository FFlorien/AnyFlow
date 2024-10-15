package be.florien.anyflow.common.ui.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.databinding.FragmentImageDisplayBinding

class ImageDisplayFragment(private var url: String) : DialogFragment() {
    private var binding: FragmentImageDisplayBinding? = null

    init {
        val args = arguments
        if (args != null) {
            url = args.getString(ARG_URL, "")
        } else {
            arguments = Bundle().apply {
                putString(ARG_URL, url)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflate = FragmentImageDisplayBinding.inflate(inflater, container, false)
        binding = inflate
        inflate.url = ImageConfig(url, null)
        return inflate.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.root?.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.bg_transparent_ripple,
                null
            )
        )
        isCancelable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

private const val ARG_URL = "url"