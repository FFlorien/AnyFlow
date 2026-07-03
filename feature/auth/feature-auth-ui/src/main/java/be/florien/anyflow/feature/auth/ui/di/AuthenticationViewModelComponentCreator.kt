package be.florien.anyflow.feature.auth.ui.di

interface AuthenticationViewModelComponentCreator {
    fun createUserConnectComponent(): AuthenticationViewModelComponent?
}