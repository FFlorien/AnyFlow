package be.florien.anyflow.feature.player.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch

abstract class InfoViewModel<T> : BaseViewModel() {
    val infoRows: LiveData<List<InfoActions.InfoRow>> = MutableLiveData(listOf())
    private val expandedSections = mutableListOf<InfoActions.FieldType>()
    abstract val infoActions: InfoActions<T>

    /**
     * Abstract methods
     */

    abstract fun getInfoRowList(): MutableList<InfoActions.InfoRow>

    abstract fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow>

    abstract fun executeInfoAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    )

    abstract fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow>


    /**
     * Public methods
     */

    open fun executeAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    ) {
        viewModelScope.launch {
            when (actionType) {
                InfoActions.ActionType.EXPANDABLE_TITLE, InfoActions.ActionType.EXPANDED_TITLE -> toggleExpansion(
                    fieldType
                )
                else -> executeInfoAction(fieldType, actionType)
            }
        }
    }

    protected fun updateRows() {
        val mutableList = getInfoRowList()
        for (fieldType in expandedSections) {
            val togglePosition =
                mutableList.indexOfFirst { it.actionType == InfoActions.ActionType.EXPANDABLE_TITLE && it.fieldType == fieldType }
            val toggledItem = mutableList.removeAt(togglePosition)
            val newToggledItem = InfoActions.InfoRow(
                toggledItem.title,
                toggledItem.text,
                null,
                toggledItem.fieldType,
                InfoActions.ActionType.EXPANDED_TITLE
            )
            mutableList.addAll(togglePosition, getActionsRows(fieldType))
            mutableList.add(togglePosition, newToggledItem)
        }
        infoRows.mutable.value = mapActionsRows(mutableList)
    }
    /**
     * Private methods: actions
     */

    private fun toggleExpansion(fieldType: InfoActions.FieldType) {
        if (!expandedSections.remove(fieldType)) {
            expandedSections.add(fieldType)
        }
        updateRows()
    }
}