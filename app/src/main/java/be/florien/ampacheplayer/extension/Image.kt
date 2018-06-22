package be.florien.ampacheplayer.extension

import android.databinding.BindingAdapter
import android.widget.ImageView
import be.florien.ampacheplayer.R
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyAppGlideModule : AppGlideModule()

@BindingAdapter("coverImageUrl")
fun ImageView.setCoverImageUrl(url: String?) {
    if (url == null) {
        setImageBitmap(null)
    }
    GlideApp.with(this.rootView)
            .load(url)
            .placeholder(R.drawable.cover_placeholder)
            .error(R.drawable.cover_placeholder)
            .fitCenter()
            .into(this)
}