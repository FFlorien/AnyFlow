package be.florien.anyflow.common.ui.data

import android.os.Parcelable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize


@Parcelize
@Immutable
data class ImageConfig(
    val url: String?,
    @param:DrawableRes val resource: Int?,
    val stateIfNone: Int = View.GONE
) :
    Parcelable