package be.florien.anyflow.extension

import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import be.florien.anyflow.R

@BindingAdapter("isSelectedBackground")
fun View.isSelectedBackground(isSelected: Boolean) {
    val color = if (isSelected) {
        ResourcesCompat.getColor(resources, R.color.selected, null)
    } else {
        ResourcesCompat.getColor(resources, R.color.unselected, null)
    }
    setBackgroundColor(color)
}

@BindingAdapter("isVisiblePresent")
fun View.isVisiblePresent(isVisible: Boolean) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

