package be.florien.anyflow.common.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.common.ui.data.info.InfoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class InfoViewModel<T, IA: InfoActions<T>> : BaseViewModel() {
    val infoRows: LiveData<List<InfoActions.InfoRow>> = MutableLiveData(listOf())
    abstract val infoActions: IA

    /**
     * Abstract methods
     */

    abstract suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow>

    abstract fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow>

    abstract fun executeAction(row: InfoActions.InfoRow): Boolean

    /**
     * Public methods
     */

    fun updateRows() {
        viewModelScope.launch {
            val mutableList = getInfoRowList()
            withContext(Dispatchers.Main) {
                infoRows.mutable.value = mapActionsRows(mutableList)
            }
        }
    }

    open fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList
}