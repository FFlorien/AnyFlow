package be.florien.anyflow.component.image.display

import android.content.Context
import be.florien.anyflow.common.image.ChangingTokenUrl
import be.florien.anyflow.common.ui.domain.ImageConfig
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer

fun displayImageFullScreen(context: Context, config: ImageConfig){
    val url = config.url ?: ""
    val resource = config.resource
    StfalconImageViewer.Builder(context, listOf(url)) { view, image ->
        Glide.with(view.rootView)
            .load(ChangingTokenUrl(image))
            .let {
                if (resource != null) {
                    it
                        .placeholder(resource)
                        .error(be.florien.anyflow.common.image.R.drawable.cover_placeholder)
                } else {
                    it
                }
            }
            .fitCenter()
            .into(view)
    }
        .withBackgroundColorResource(R.color.cardview_shadow_start_color)
        .show()
}