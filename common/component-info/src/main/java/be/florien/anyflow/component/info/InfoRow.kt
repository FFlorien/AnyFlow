package be.florien.anyflow.component.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig

sealed class InfoRow(
    @StringRes open val title: Int,
    open val text: TextConfig,
    open val image: ImageConfig,
    @DrawableRes open val icon: Int?,
) {
    var tag: Any? = null //todo test again with tag in constructor

    data class BasicInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig
    ) : InfoRow(title, text, image, null)

    data class ActionInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig) :
        InfoRow(title, text, image, null)

    data class NavigationInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig) :
        InfoRow(title, text, image, R.drawable.ic_go)

    data class ContainerInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        val subRows: List<InfoRow>
    ) : InfoRow(title, text, image, R.drawable.ic_next_occurence)

    data class ProgressInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
    ) : InfoRow(title, text, image, null) {
        var progress: LiveData<Double>? = null
        var secondaryProgress: LiveData<Double>? = null
    }

    data class ShortcutInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig) :
        InfoRow(title, text, image, null) // todo handle shortcut...
}