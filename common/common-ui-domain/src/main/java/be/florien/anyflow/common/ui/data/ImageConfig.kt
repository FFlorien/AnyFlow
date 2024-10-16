package be.florien.anyflow.common.ui.data

import android.os.Parcelable
import android.view.View
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize


@Parcelize
data class ImageConfig(val url: String?, @DrawableRes val resource: Int?, val stateIfNone: Int = View.GONE):
    Parcelable