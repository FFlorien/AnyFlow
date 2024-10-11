package be.florien.anyflow.feature.player.ui.di

interface PlayerActivityComponentCreator {
    fun createPlayerActivityComponent(): PlayerActivityComponent?
    fun isUserConnected(): Boolean
}