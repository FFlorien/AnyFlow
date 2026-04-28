package be.florien.anyflow.common.base

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.data.TextConfig

fun Activity.getDisplayWidth(): Int {
    return if (Build.VERSION.SDK_INT >= 30) {
        getDisplayWidthNew()
    } else {
        getDisplayWidthLegacy()
    }
}

@Suppress("DEPRECATION")
private fun Activity.getDisplayWidthLegacy(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Activity.getDisplayWidthNew() = windowManager.currentWindowMetrics.bounds.width()

fun RecyclerView.refreshVisibleViewHolders(updateVH: (RecyclerView.ViewHolder) -> Unit) {
    val firstPosition =
        (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
            ?: (layoutManager as? GridLayoutManager)?.findFirstVisibleItemPosition()
            ?: return
    val lastPosition =
        (layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
            ?: (layoutManager as? GridLayoutManager)?.findLastVisibleItemPosition()
            ?: return
    for (position in firstPosition..lastPosition) {
        val viewHolder = findViewHolderForAdapterPosition(position)
        viewHolder?.let { updateVH(it) }
    }
}

@BindingAdapter("textConfig")
fun TextView.seTextConfigBinding(textConfig: TextConfig?) {
    text = textConfig?.getText(context.resources)?.parseAsHtml() ?: ""
}

@BindingAdapter(value = ["htmlText"])
fun TextView.setHtmlText(string: String?) {
    text = HtmlCompat.fromHtml(string ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT)
}