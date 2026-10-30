package com.example.enkan_haban.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed interface Route {
    /** Top-level destinations shown in the bottom bar. */
    sealed interface Tab : Route

    data object Home : Tab
    data object Sell : Tab
    data object Cart : Tab
    data object Account : Tab

    data class ProductDetail(val productId: String) : Route

    /**
     * The seller editing their own ad. [visit] differs on every open, so each visit gets a
     * fresh form instead of unsaved edits left over from the last one.
     */
    data class EditListing(val productId: String, val visit: Int = nextVisit++) : Route

    private companion object {
        var nextVisit = 0
    }
}

/** Minimal back-stack navigator. Home is always the root. */
class Navigator {
    private val backStack: SnapshotStateList<Route> = mutableStateListOf(Route.Home)

    val current: Route get() = backStack.last()

    /** The tab the current screen belongs to, for highlighting the bottom bar. */
    val currentTab: Route.Tab get() = backStack.last { it is Route.Tab } as Route.Tab

    val canGoBack: Boolean get() = backStack.size > 1

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun selectTab(tab: Route.Tab) {
        backStack.clear()
        backStack.add(Route.Home)
        if (tab != Route.Home) backStack.add(tab)
    }

    /** Returns false when already at the root, so the platform can close the app. */
    fun goBack(): Boolean {
        if (!canGoBack) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}
