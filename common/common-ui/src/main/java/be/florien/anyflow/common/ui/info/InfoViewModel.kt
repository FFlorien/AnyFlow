package be.florien.anyflow.common.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class InfoViewModel<T> : BaseViewModel() {
    val infoRows: LiveData<List<T>> = MutableLiveData(listOf())

    /**
     * Abstract methods
     */

    abstract suspend fun getInfoRowList(): MutableList<T>

    abstract fun executeAction(row: T): Boolean

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

    open fun mapActionsRows(initialList: List<T>): List<T> =
        initialList //todo find a way to get rid of this
}