package be.florien.anyflow.feature.alarms

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.feature.alarms.add.AddAlarmFragment
import be.florien.anyflow.feature.alarms.list.AlarmListFragment
import be.florien.anyflow.feature.menu.implementation.AddAlarmMenuHolder
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.MenuHolder


class AlarmActivity : AppCompatActivity() {
    lateinit var toolbar: Toolbar
    private lateinit var viewModel: AlarmViewModel

    val menuCoordinator = MenuCoordinator()
    private lateinit var addMenu: MenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[AlarmViewModel::class.java]
        anyFlowApp.serverComponent?.inject(viewModel)
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