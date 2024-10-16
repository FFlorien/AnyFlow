package be.florien.anyflow.common.di

import android.app.Activity


val Activity.viewModelFactory: AnyFlowViewModelFactory
    get() = (this as ViewModelFactoryProvider).viewModelFactory

interface ViewModelFactoryProvider {
    val viewModelFactory: AnyFlowViewModelFactory
}