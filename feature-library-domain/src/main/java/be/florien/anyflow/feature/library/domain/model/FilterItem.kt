package be.florien.anyflow.feature.library.domain.model


class FilterItem(
    val id: Long,
    val displayName: String,
    val isSelected: Boolean,
    val artUrl: String? = null
)