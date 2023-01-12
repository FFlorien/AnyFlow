package be.florien.anyflow.extension

import android.graphics.PorterDuff
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import android.widget.TimePicker
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import be.florien.anyflow.R
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import kotlinx.parcelize.Parcelize


@GlideModule
class MyAppGlideModule : AppGlideModule()

@BindingAdapter("imageSource")
fun ImageView.setImageSource(config: ImageConfig?) {
    if (config?.url != null) {
        GlideApp.with(this.rootView)
            .load(ChangingTokenUrl(config.url))
            .let {
                if (config.resource != null) {
                    it.placeholder(config.resource)
                        .error(R.drawable.cover_placeholder)
                } else {
                    it
                }
            }
            .fitCenter()
            .into(this)
        visibility = View.VISIBLE
        imageTintMode = PorterDuff.Mode.DST
    } else if (config?.resource != null) {
        setImageResource(config.resource)
        visibility = View.VISIBLE
        imageTintMode = PorterDuff.Mode.SRC_IN
    } else {
        setImageBitmap(null)
        visibility = config?.stateIfNone ?: View.GONE
    }
}

@Parcelize
data class ImageConfig(val url: String?, @DrawableRes val resource: Int?, val stateIfNone: Int = View.GONE): Parcelable

internal class ChangingTokenUrl(val url: String) : GlideUrl(url) {
    override fun getCacheKey(): String {
        val uri = Uri.parse(url)
        return (uri.host
            ?.plus(uri.getQueryParameter("type"))
            ?.plus("_")
            ?.plus(uri.getQueryParameter("id")))
            ?: url
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

@BindingAdapter("time")
fun TimePicker.setTimeBinding(time: Int) {
    hour = time / 60
    minute = time % 60
}

@InverseBindingAdapter(attribute = "time")
fun TimePicker.getHourBinding(): Int {
    return hour * 60 + minute
}

@BindingAdapter("app:timeAttrChanged")
fun setListenersForHour(
    view: TimePicker,
    attrChange: InverseBindingListener
) {
    view.setOnTimeChangedListener { _, _, _ -> attrChange.onChange() }
}