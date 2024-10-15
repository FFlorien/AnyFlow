package be.florien.anyflow.feature.alarm.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.common.ui.menu.MenuHolder
import be.florien.anyflow.feature.alarm.ui.add.AddAlarmFragment
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponentCreator
import be.florien.anyflow.feature.alarm.ui.list.AlarmListFragment
import javax.inject.Inject


class AlarmActivity : AppCompatActivity(), ViewModelFactoryProvider {
    lateinit var toolbar: Toolbar
    private lateinit var viewModel: AlarmViewModel

    val menuCoordinator = MenuCoordinator()
    private lateinit var addMenu: MenuHolder

    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityComponent = (application as AlarmActivityComponentCreator)
            .createAlarmActivityComponent()
            ?: throw IllegalStateException()
        activityComponent.inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory)[AlarmViewModel::class.java]
        setContentView(R.layout.activity_alarms)
        toolbar = findViewById(R.id.toolbar)

        initToolbar()
        initMenus()

        val fragment = if (viewModel.shouldAskPermission()) {
            AlarmPermissionFragment()
        } else {
            AlarmListFragment()
        }
        supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuCoordinator.inflateMenus(menu, menuInflater)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menuCoordinator.prepareMenus(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onResume() {
        super.onResume()
        updateMenuItemVisibility()
        adaptToolbarToCurrentFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(addMenu)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            adaptToolbarToCurrentFragment()
        }
        toolbar.setNavigationOnClickListener {
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
        }
    }

    private fun initMenus() {
        addMenu = AddAlarmMenuHolder {
            supportFragmentManager.beginTransaction().replace(R.id.container, AddAlarmFragment())
                .addToBackStack(null).commit()
        }

        menuCoordinator.addMenuHolder(addMenu)
    }

    private fun updateMenuItemVisibility() {
        val isList = supportFragmentManager.findFragmentById(R.id.container) is AlarmListFragment
        addMenu.isVisible = isList
    }

    private fun adaptToolbarToCurrentFragment() {
        (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()
            ?.let {
                supportActionBar?.title = it
            }
    }
}