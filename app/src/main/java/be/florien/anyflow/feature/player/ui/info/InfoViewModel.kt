package be.florien.anyflow.feature.player.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class InfoViewModel<T> : BaseViewModel() {
    val infoRows: LiveData<List<InfoActions.InfoRow>> = MutableLiveData(listOf())
    private val expandedSections = mutableListOf<InfoActions.InfoRow>()
    abstract val infoActions: InfoActions<T>

    /**
     * Abstract methods
     */

    abstract suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow>

    abstract suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow>

    abstract fun executeAction(row: InfoActions.InfoRow): Boolean

    /**
     * Public methods
     */

    fun updateRows() {
        viewModelScope.launch {
            val mutableList = getInfoRowList()
            for (expandedRow in expandedSections) {
                val togglePosition = mutableList.indexOfFirst {
                    it.actionType == SongInfoActions.SongActionType.ExpandableTitle
                            && it.fieldType == expandedRow.fieldType
                            && (expandedRow as? SongInfoActions.SongMultipleInfoRow)?.index == (it as? SongInfoActions.SongMultipleInfoRow)?.index
                }
                val infoRow = mutableList[togglePosition]
                val actionsRows = getActionsRowsFor(infoRow)
                mutableList.addAll(togglePosition + 1, actionsRows)
            }
            withContext(Dispatchers.Main) {
                infoRows.mutable.value = mapActionsRows(mutableList)
            }
        }
    }

    open fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList

    /**
     * Private methods: actions
     */

    protected fun toggleExpansion(row: InfoActions.InfoRow) {
        if (!expandedSections.remove(row)) {
            expandedSections.add(row)
        }
        updateRows()
    }
}