package be.florien.anyflow.feature.playlist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import be.florien.anyflow.R
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.feature.playlist.list.PlaylistListFragment

class PlaylistsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    internal val menuCoordinator = MenuCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playlists)
        toolbar = findViewById(R.id.toolbar)
        initToolbar()

        if (supportFragmentManager.findFragmentById(R.id.container) == null) {
            val fragment = PlaylistListFragment()
            supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        adaptToolbarToCurrentFragment()
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

    private fun adaptToolbarToCurrentFragment() {
        (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()
            ?.let {
                supportActionBar?.title = it
            }
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.addOnBackStackChangedListener {
            adaptToolbarToCurrentFragment()
        }

        toolbar.setNavigationOnClickListener {
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
        }
    }
}