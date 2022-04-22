package be.florien.anyflow.injection

interface ViewModelFactoryHolder {
    fun getFactory(): AnyFlowViewModelFactory
}