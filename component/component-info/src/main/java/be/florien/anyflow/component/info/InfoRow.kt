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
    open val tag: Any?
) {

    data class BasicInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any?
    ) : InfoRow(title, text, image, null, tag)

    data class ActionInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any?
    ) : InfoRow(title, text, image, null, tag)

    data class NavigationInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any?
    ) : InfoRow(title, text, image, R.drawable.ic_go, tag)

    data class ContainerInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        val subRows: List<InfoRow>,
        override val tag: Any?
    ) : InfoRow(title, text, image, R.drawable.ic_next_occurence, tag)

    data class ProgressInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any?
    ) : InfoRow(title, text, image, null, tag) {
        var progress: LiveData<Double>? = null
        var secondaryProgress: LiveData<Double>? = null

        constructor(
            title: Int,
            text: TextConfig,
            image: ImageConfig,
            tag: Any?,
            progress: LiveData<Double>?,
            secondaryProgress: LiveData<Double>?
        ) : this(title, text, image, tag) {
            this.progress = progress
            this.secondaryProgress = secondaryProgress
        }
    }

    data class ShortcutInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any?
    ) : InfoRow(title, text, image, null, tag) // todo handle shortcut...
}