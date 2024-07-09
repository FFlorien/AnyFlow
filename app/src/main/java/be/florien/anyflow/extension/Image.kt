package be.florien.anyflow.extension

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TimePicker
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.R
import be.florien.anyflow.common.ui.data.ImageConfig
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named


@BindingAdapter("time")
fun TimePicker.setTimeBinding(time: Int) {
    hour = time / 60
    minute = time % 60
}

@InverseBindingAdapter(attribute = "time")
fun TimePicker.getHourBinding(): Int {
    return hour * 60 + minute
}

@BindingAdapter("timeAttrChanged")
fun setListenersForHour(
    view: TimePicker,
    attrChange: InverseBindingListener
) {
    view.setOnTimeChangedListener { _, _, _ -> attrChange.onChange() }
}