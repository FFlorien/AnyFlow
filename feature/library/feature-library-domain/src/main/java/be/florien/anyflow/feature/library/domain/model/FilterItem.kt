package be.florien.anyflow.feature.library.domain.model


data class FilterItem(
    val id: Long,
    val title: String,
    val isSelected: Boolean,
    val section: String,
    val artUrl: String? = null,
    val duration: String? = null,
    val subtitle: String? = null,
    val subsubtitle: String? = null
)