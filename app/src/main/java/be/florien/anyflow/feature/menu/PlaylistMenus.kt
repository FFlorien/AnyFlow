package be.florien.anyflow.feature.menu

import android.content.Context
import be.florien.anyflow.R


class FilterMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_player,
        R.id.menu_filters,
        action)

class OrderMenuHolder(isOrdered: Boolean, context: Context, action: () -> Unit) : AnimatedMenuHolder(
        R.menu.menu_player,
        R.id.menu_order,
        R.drawable.ic_order_ordered,
        R.drawable.ic_order_random,
        isOrdered,
        context,
        action)