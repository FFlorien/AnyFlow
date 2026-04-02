package be.florien.anyflow.feature.library.ui

import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.FilterDisplay

fun FilterItem.toDisplay(isSelected: Boolean) = FilterDisplay(
    id = id,
    title = title,
    isSelected = isSelected,
    section = section,
    artUrl = artUrl,
    duration = duration,
    subtitle = subtitle,
    subSubtitle = subsubtitle
)

fun FilterDisplay.toItem() = FilterItem(
    id = id,
    title = title,
    isSelected = isSelected,
    section = section,
    artUrl = artUrl,
    duration = duration,
    subtitle = subtitle,
    subsubtitle = subSubtitle
)