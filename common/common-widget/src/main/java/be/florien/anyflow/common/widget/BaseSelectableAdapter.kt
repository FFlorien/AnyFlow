package be.florien.anyflow.common.widget

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