package be.florien.anyflow.feature.library.ui.list

interface LibraryListViewModelClassProvider {
    fun getViewModelClass(filterName: String): Class<out LibraryListViewModel>
}