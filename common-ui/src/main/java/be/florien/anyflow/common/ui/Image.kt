package be.florien.anyflow.common.ui

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.di.GlideModuleInjectorContainer
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


@GlideModule
class MyAppGlideModule : AppGlideModule() {
    @Inject
    @Named("glide")
    lateinit var okHttp: OkHttpClient

    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)

        val glideModuleInjector  = (context.applicationContext as GlideModuleInjectorContainer).glideModuleInjector
        glideModuleInjector?.inject(this)
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttp))
    }
}

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

@BindingAdapter("imageSource")
fun ImageView.setImageSource(config: ImageConfig?) {
    val url = config?.url
    val resource = config?.resource
    if (url != null) {
        GlideApp.with(this.rootView)
            .load(ChangingTokenUrl(url))
            .let {
                if (resource != null) {
                    it.placeholder(resource)
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