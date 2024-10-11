package be.florien.anyflow.feature.songlist.ui

import android.content.Context
import be.florien.anyflow.common.ui.menu.AnimatedMenuHolder


class OrderMenuHolder(isOrdered: Boolean, context: Context, action: () -> Unit) : AnimatedMenuHolder(
    R.menu.menu_player,
    R.id.menu_order,
    R.drawable.ic_order_ordered,
    R.drawable.ic_order_random,
    isOrdered,
    context,
    action)