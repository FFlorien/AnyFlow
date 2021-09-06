package be.florien.anyflow.extension

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import be.florien.anyflow.R
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule


@GlideModule
class MyAppGlideModule : AppGlideModule()

@BindingAdapter("coverImageUrl")
fun ImageView.setCoverImageUrl(url: String?) {
    if (url == null) {
        setImageBitmap(null)
    } else {
        GlideApp.with(this.rootView)
                .load(url)
                .placeholder(R.drawable.cover_placeholder)
                .error(R.drawable.cover_placeholder)
                .fitCenter()
                .into(this)
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