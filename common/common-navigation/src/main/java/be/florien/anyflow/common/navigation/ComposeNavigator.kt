package be.florien.anyflow.common.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class ComposeNavigator(val state: NavigationState) {
    fun navigate(route: NavKey, parentTopRoute: NavKey? = null, clearBackStack: Boolean = false) {
        val topRoute = parentTopRoute ?: state.topLevelRoute
        val currentStack = state.backStacks[topRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        if (clearBackStack) {
            currentStack.removeAll { it !in state.backStacks.keys }
        }

        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
        } else {
            if (topRoute != state.topLevelRoute) {
                state.topLevelRoute = topRoute
            }
            currentStack.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute || currentStack.size == 1) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
