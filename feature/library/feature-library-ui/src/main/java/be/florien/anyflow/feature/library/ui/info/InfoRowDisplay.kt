package be.florien.anyflow.feature.library.ui.info

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig


@Immutable
data class InfoRowDisplay(
    val leftImage: ImageConfig?,
    @StringRes
    val title: Int,
    val info: TextConfig,
    @DrawableRes
    val actionIcon: Int?,
    @field:ColorRes
    val backgroundColor: Int?
)