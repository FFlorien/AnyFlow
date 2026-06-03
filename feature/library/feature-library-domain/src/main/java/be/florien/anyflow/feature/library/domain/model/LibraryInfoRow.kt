package be.florien.anyflow.feature.library.domain.model


data class LibraryInfoRow(
    val fieldType: LibraryFieldType,
    val rowType: LibraryRowType,
    val count: Int
)