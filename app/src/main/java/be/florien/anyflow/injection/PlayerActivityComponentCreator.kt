package be.florien.anyflow.injection

interface PlayerActivityComponentCreator {
    fun createPlayerActivityComponent(): PlayerActivityComponent?
    fun isUserConnected(): Boolean
}