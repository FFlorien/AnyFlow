package be.florien.anyflow.feature.alarms

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityAlarmsBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.alarms.add.AddAlarmFragment
import be.florien.anyflow.feature.menu.AddAlarmMenuHolder
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.MenuHolder


class AlarmActivity : AppCompatActivity() {
    lateinit var binding: ActivityAlarmsBinding
    private lateinit var viewModel: AlarmViewModel

    val menuCoordinator = MenuCoordinator()
    private lateinit var addMenu: MenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(AlarmViewModel::class.java)
        anyFlowApp.applicationComponent.inject(viewModel)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_alarms)
        binding.lifecycleOwner = this

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
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cancel)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            adaptToolbarToCurrentFragment()
        }
    }

    private fun initMenus() {
        addMenu = AddAlarmMenuHolder {
            supportFragmentManager.beginTransaction().replace(R.id.container, AddAlarmFragment()).addToBackStack(null).commit()
        }

        menuCoordinator.addMenuHolder(addMenu)
    }

    private fun updateMenuItemVisibility() {
        val isList = supportFragmentManager.findFragmentById(R.id.container) is AlarmListFragment
        addMenu.isVisible = isList
    }

    private fun adaptToolbarToCurrentFragment() {
        (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()?.let {
            supportActionBar?.title = it
        }
    }
}