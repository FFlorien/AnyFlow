package be.florien.anyflow.feature.library.domain.model

import be.florien.anyflow.common.ui.domain.TextConfig


data class LibraryInfoRow(
    val fieldType: LibraryFieldType,
    val actionType: LibraryRowType,
    val infoText: TextConfig)