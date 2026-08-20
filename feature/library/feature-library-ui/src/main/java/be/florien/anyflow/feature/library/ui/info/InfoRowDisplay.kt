package be.florien.anyflow.feature.library.ui.info

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryRowType


@Immutable
data class InfoRowDisplay(
    val id: Long,
    val key: String,
    val leftImage: ImageConfig?,
    @field:StringRes
    val title: Int,
    val info: TextConfig,
    @field:ColorRes
    val backgroundColor: Int?,
    val rowType: LibraryRowType,
    val fieldType: LibraryFieldType
)