package be.florien.anyflow.injection

import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory

interface ViewModelFactoryHolder {
    fun getFactory(): AnyFlowViewModelFactory
}