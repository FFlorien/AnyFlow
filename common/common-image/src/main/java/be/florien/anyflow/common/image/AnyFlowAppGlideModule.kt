package be.florien.anyflow.common.image

import android.content.Context
import be.florien.anyflow.common.image.di.GlideModuleInjectorContainer
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
class AnyFlowAppGlideModule : AppGlideModule() {
    @Inject
    @Named("glide")
    lateinit var okHttp: OkHttpClient

    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)

        val glideModuleInjector  = (context.applicationContext as GlideModuleInjectorContainer).glideModuleInjector
        glideModuleInjector?.inject(this)
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(okHttp)
        )
    }
}