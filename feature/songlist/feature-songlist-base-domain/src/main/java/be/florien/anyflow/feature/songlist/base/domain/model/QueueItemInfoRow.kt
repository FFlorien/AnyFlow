package be.florien.anyflow.feature.songlist.base.domain.model

import androidx.annotation.DrawableRes

interface QueueItemFieldType {
    @get:DrawableRes
    val iconRes: Int
}

interface QueueItemActionType {
    @get:DrawableRes
    val iconRes: Int
}

interface QueueItemInfoRow<FT : QueueItemFieldType, AT : QueueItemActionType> {
    val fieldType: FT
    val actionType: AT
}