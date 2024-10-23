package be.florien.anyflow.common.ui.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig

sealed class InfoRow(
    @StringRes open val title: Int,
    open val text: TextConfig,
    open val image: ImageConfig,
    @DrawableRes open val icon: Int?,
    open val tag: Any
) {
    open fun areRowTheSame(other: InfoRow): Boolean {
        return text == other.text && (image == other.image) && this.javaClass == other.javaClass
    }

    data class BasicInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any
    ) : InfoRow(title, text, image, null, tag)

    data class ActionInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, null, tag)

    data class NavigationInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, R.drawable.ic_go, tag)

    data class ContainerInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any,
        val subRows: List<InfoRow>
    ) : InfoRow(title, text, image, R.drawable.ic_next_occurence, tag)

    data class ProgressInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any,
        val progress: LiveData<Double>,
        val secondaryProgress: LiveData<Double>
    ) : InfoRow(title, text, image, null, tag) {
        override fun equals(other: Any?) =
            other is ProgressInfoRow && title == other.title && text == other.text && image == other.image && tag == other.tag

        override fun hashCode() =
            title.hashCode() + text.hashCode() + image.hashCode() + tag.hashCode()
    }

    data class ShortcutInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, null, tag)
}