package be.florien.anyflow.feature.library.domain.model

import be.florien.anyflow.common.ui.data.TextConfig


data class FilterItem(
    val id: Long,
    val title: TextConfig,
    val isSelected: Boolean,
    val artUrl: String? = null,
    val duration: String? = null,
    val subtitle: TextConfig? = null,
    val subsubtitle: TextConfig? = null
)