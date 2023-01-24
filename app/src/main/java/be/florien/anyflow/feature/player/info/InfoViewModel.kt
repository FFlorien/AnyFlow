package be.florien.anyflow.feature.player.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class InfoViewModel<T> : BaseViewModel() {
    val infoRows: LiveData<List<InfoActions.InfoRow>> = MutableLiveData(listOf())
    private val expandedSections = mutableListOf<InfoActions.FieldType>()
    abstract val infoActions: InfoActions<T>

    /**
     * Abstract methods
     */

    abstract suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow>

    abstract suspend fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow>

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
                is InfoActions.ActionType.ExpandableTitle, is InfoActions.ActionType.ExpandedTitle -> toggleExpansion(
                    fieldType
                )
                else -> executeInfoAction(fieldType, actionType)
            }
        }
    }

    fun updateRows() {
        viewModelScope.launch {
            val mutableList = getInfoRowList()
            for (fieldType in expandedSections) {
                val togglePosition =
                    mutableList.indexOfFirst { it.actionType is InfoActions.ActionType.ExpandableTitle && it.fieldType == fieldType }
                val toggledItem = mutableList.removeAt(togglePosition)
                val newToggledItem = InfoActions.InfoRow(
                    toggledItem.title,
                    toggledItem.text,
                    null,
                    toggledItem.fieldType,
                    InfoActions.ActionType.ExpandedTitle()
                )
                val actionsRows = getActionsRows(fieldType)
                mutableList.addAll(togglePosition, actionsRows)
                mutableList.add(togglePosition, newToggledItem)
            }
            withContext(Dispatchers.Main) {
                infoRows.mutable.value = mapActionsRows(mutableList)
            }
        }
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