package be.florien.anyflow.common.image

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import be.florien.anyflow.common.ui.data.ImageConfig

@BindingAdapter("imageSource")
fun ImageView.setImageSource(config: ImageConfig?) {
    val url = config?.url
    val resource = config?.resource
    if (url != null) {
        GlideApp.with(this.rootView)
            .load(ChangingTokenUrl(url))
            .let {
                if (resource != null) {
                    it
                        .placeholder(resource)
                        .error(R.drawable.cover_placeholder)
                } else {
                    it
                }
            }
            .fitCenter()
            .into(this)
        visibility = View.VISIBLE
        imageTintMode = PorterDuff.Mode.DST
    } else if (resource != null) {
        setImageResource(resource)
        visibility = View.VISIBLE
        imageTintMode = PorterDuff.Mode.SRC_IN
    } else {
        setImageBitmap(null)
        visibility = config?.stateIfNone ?: View.GONE
    }
}

@BindingAdapter("android:drawableResource")
fun setImageResource(imageView: ImageView, resource: Int) {
    if (resource == 0) {
        imageView.setImageBitmap(null)
    } else {
        imageView.setImageResource(resource)
    }
}

@BindingAdapter("isVisiblePresent")
fun View.isVisiblePresent(isVisible: Boolean) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}