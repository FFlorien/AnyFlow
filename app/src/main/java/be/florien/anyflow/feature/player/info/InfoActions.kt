package be.florien.anyflow.feature.player.info

import androidx.annotation.StringRes

abstract class InfoActions<T> {

    abstract suspend fun getInfoRows(infoSource: T): List<InfoRow>

    abstract suspend fun getActionsRows(
        infoSource: T,
        row: InfoRow
    ): List<InfoRow>

    abstract class InfoRow(
        @StringRes open val title: Int,
        open val text: String?,
        @StringRes open val textRes: Int?,
        open val fieldType: FieldType,
        open val actionType: ActionType,
        open val imageUrl: String?
    ) {
        open fun areRowTheSame(other: InfoRow): Boolean {
            return fieldType == other.fieldType && (actionType == other.actionType)
        }

        open fun areContentTheSame(other: InfoRow): Boolean =
            areRowTheSame(other) && actionType == other.actionType
    }

    interface FieldType {
        val iconRes: Int
        val couldGetUrl: Boolean
    }

    interface ActionType {
        val iconRes: Int
        val category: ActionTypeCategory
    }

    enum class ActionTypeCategory {
        Navigation,
        Action,
        None;
    }
}