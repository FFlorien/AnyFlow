package be.florien.anyflow.feature.library.ui

import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.FilterDisplay

fun FilterItem.toDisplay(isSelected: Boolean) = FilterDisplay(
        id,
        title.getText(),
        isSelected,
        artUrl,
        duration,
        subtitle?.getText(),
        subsubtitle?.getText()
)

fun FilterDisplay.toItem() = FilterItem(
        id,
    TextConfig(title),
        isSelected,
        artUrl,
        duration,
        subtitle?.let { TextConfig(it) },
        subSubtitle?.let { TextConfig(it) }
)