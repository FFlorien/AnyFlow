package be.florien.anyflow.feature.library.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.res.ResourcesCompat
import androidx.paging.filter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.list.DetailViewHolderListener
import be.florien.anyflow.common.ui.list.ItemInfoTouchAdapter
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.feature.library.ui.menu.SearchMenuHolder
import be.florien.anyflow.feature.library.ui.menu.SelectAllMenuHolder
import be.florien.anyflow.feature.library.ui.menu.SelectNoneMenuHolder
import be.florien.anyflow.management.filters.model.Filter
import com.google.android.material.snackbar.Snackbar

abstract class LibraryListFragment @SuppressLint("ValidFragment")
constructor(
    var filterType: String,
    private var parentFilter: Filter<*>? = null
) : BaseFilteringFragment(),
    DetailViewHolderListener<FilterItem> {

    companion object {
        private const val FILTER_TYPE = "TYPE"
        private const val PARENT_FILTER = "PARENT_FILTER"
    }

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: be.florien.anyflow.common.navigation.Navigator
        get() = viewModel.navigator
    lateinit var viewModel: LibraryListViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterBinding
    private val searchMenuHolder by lazy {
        SearchMenuHolder(viewModel.isSearching.value == true, requireContext()) {
            val currentValue = viewModel.isSearching.value ?: false
            viewModel.isSearching.value = !currentValue
        }
    }
    private val selectAllMenuHolder by lazy {
        SelectAllMenuHolder {
            viewModel.selectAllInSelection()
        }
    }
    private val selectNoneMenuHolder by lazy {
        SelectNoneMenuHolder {
            viewModel.selectNoneInSelection()
        }
    }

    abstract fun getViewModel(filterName: String): LibraryListViewModel

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, "Error")
            parentFilter = it.getParcelable(PARENT_FILTER)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
                putParcelable(PARENT_FILTER, parentFilter)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(searchMenuHolder)
        menuCoordinator.addMenuHolder(selectAllMenuHolder)
        menuCoordinator.addMenuHolder(selectNoneMenuHolder)
        viewModel.isSearching.observe(this) {
            searchMenuHolder.changeState(!it)
            val imm: InputMethodManager? =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    fragmentBinding.search.requestFocus()
                    imm?.showSoftInput(fragmentBinding.search, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            } else {
                imm?.hideSoftInputFromWindow(fragmentBinding.root.windowToken, 0)
            }
        }
        viewModel.hasFilterOfThisType.observe(this) { isAnythingSelected ->
            selectAllMenuHolder.isVisible = !isAnythingSelected
            selectNoneMenuHolder.isVisible = isAnythingSelected
        }
        viewModel.errorMessage.observe(this) {
            if (it > 0) {
                Snackbar.make(fragmentBinding.filterList, it, Snackbar.LENGTH_SHORT).show()
                viewModel.errorMessage.value = -1
            }
        }
        searchMenuHolder.isVisible = viewModel.hasSearch
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = getViewModel(filterType)
        viewModel.navigationFilter = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        fragmentBinding.filterList.adapter = FilterListAdapter(
            viewModel::hasFilter,
            viewModel::toggleFilterSelection,
            this
        )
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.filterList.addItemDecoration(
                    DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
                        .apply { setDrawable(it) })
            }
        viewModel.values.observe(viewLifecycleOwner) { pagingData ->
            val pagingDataNew = pagingData.filter {
                !viewModel.shouldFilterOut(it)
            }
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(
                lifecycle,
                pagingDataNew
            )
            fragmentBinding.filterList.addOnItemTouchListener(object : ItemInfoTouchAdapter(),
                RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return onInterceptTouchEvent(e)
                }

                override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                    val childView = rv.findChildViewUnder(downTouchX, downTouchY) ?: return
                    val viewHolder =
                        (rv.findContainingViewHolder(childView) as? FilterViewHolder) ?: return
                    if (!onTouch(viewHolder, event)) {
                        viewHolder.swipeToClose()
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                }
            })
        }
        return fragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(searchMenuHolder)
        menuCoordinator.removeMenuHolder(selectAllMenuHolder)
        menuCoordinator.removeMenuHolder(selectNoneMenuHolder)
    }
}
