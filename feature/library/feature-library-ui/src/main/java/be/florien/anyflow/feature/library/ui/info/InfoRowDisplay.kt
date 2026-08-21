package be.florien.anyflow.feature.library.ui.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryRowType


@Immutable
sealed interface InfoRowDisplay {
    val key: String
    @get:StringRes
    val title: Int

    @Immutable
    data class List(
        override val key: String,
        @field:StringRes
        override val title: Int,
        @field:DrawableRes
        val leftImage: Int,
        val countText: TextConfig,
    ) : InfoRowDisplay

    @Immutable
    data class Item(
        override val key: String,
        @field:StringRes
        override val title: Int,
        val id: Long,
        val leftImage: ImageConfig,
        val info: TextConfig,
        val rowType: LibraryRowType,
        val fieldType: LibraryFieldType
    ) : InfoRowDisplay

    @Immutable
    data class Action(
        override val key: String,
        @field:StringRes
        override val title: Int,
        val actionDescription: TextConfig,
        val rowType: LibraryRowType,
        val fieldType: LibraryFieldType
    ) : InfoRowDisplay
}