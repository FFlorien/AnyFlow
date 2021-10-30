package be.florien.anyflow.extension

import android.widget.ImageView
import android.widget.TimePicker
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
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

@BindingAdapter("time")
fun TimePicker.setTimeBinding(time: Int) {
    currentHour = time / 60
    currentMinute = time % 60
}

@InverseBindingAdapter(attribute = "time")
fun TimePicker.getHourBinding(): Int {
    return currentHour * 60 + currentMinute
}
@BindingAdapter("app:timeAttrChanged")
fun setListenersForHour(
        view: TimePicker,
        attrChange: InverseBindingListener
) {
    view.setOnTimeChangedListener { _, _, _ -> attrChange.onChange() }
}