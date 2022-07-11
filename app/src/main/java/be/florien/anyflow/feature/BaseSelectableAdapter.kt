package be.florien.anyflow.feature

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

interface BaseSelectableAdapter<ID> {
    val isSelected: (ID) -> Boolean
    val setSelected: (ID) -> Unit

    interface BaseSelectableViewHolder<ID, ITEM> {
        val onSelectChange: (ID) -> Unit

        fun bind(item: ITEM, isSelected: Boolean)

        fun setSelection(isSelected: Boolean)

        fun getCurrentId(): ID?
    }
}

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