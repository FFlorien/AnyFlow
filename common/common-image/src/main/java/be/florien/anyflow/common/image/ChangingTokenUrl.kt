package be.florien.anyflow.common.image

import android.net.Uri
import com.bumptech.glide.load.model.GlideUrl

class ChangingTokenUrl(val url: String) : GlideUrl(url) {
    override fun getCacheKey(): String {
        val uri = Uri.parse(url)
        return (uri.host
            ?.plus(uri.getQueryParameter("type"))
            ?.plus("_")
            ?.plus(uri.getQueryParameter("id")))
            ?: url
    }
}