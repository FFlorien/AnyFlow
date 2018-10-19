package be.florien.anyflow.view.menu

import android.content.Context
import be.florien.anyflow.R


class FilterMenuHolder(isUnfiltered: Boolean, context: Context, action: () -> Unit): AnimatedMenuHolder(
        R.menu.menu_player,
        R.id.menu_filters,
        R.drawable.ic_filter_unfiltered,
        R.drawable.ic_filter_filtered,
        isUnfiltered,
        context,
        action)

class OrderMenuHolder(isOrdered: Boolean, context: Context, action: () -> Unit): AnimatedMenuHolder(
        R.menu.menu_player,
        R.id.menu_order,
        R.drawable.ic_order_ordered,
        R.drawable.ic_order_random,
        isOrdered,
        context,
        action)