package be.florien.anyflow.feature.auth.ui.di

interface AuthenticationActivityComponentCreator {
    fun createUserConnectComponent(): AuthenticationActivityComponent?
}