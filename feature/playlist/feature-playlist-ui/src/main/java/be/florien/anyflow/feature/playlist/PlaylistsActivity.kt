package be.florien.anyflow.feature.playlist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.feature.playlist.ui.R as ModuleR
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.component.menu.MenuCoordinator
import be.florien.anyflow.feature.playlist.di.PlaylistActivityComponentCreator
import be.florien.anyflow.feature.playlist.di.PlaylistActivityComponent
import be.florien.anyflow.feature.playlist.list.PlaylistListFragment

class PlaylistsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    internal val menuCoordinator = MenuCoordinator()
    internal lateinit var component: PlaylistActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        component = (applicationContext as PlaylistActivityComponentCreator)
            .createPlaylistComponent()
            ?: throw IllegalStateException()
        super.onCreate(savedInstanceState)

        setContentView(ModuleR.layout.activity_playlists)
        toolbar = findViewById(ModuleR.id.toolbar)
        initToolbar()

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view: View, windowInsets: WindowInsetsCompat ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                top = insets.top,
                left = insets.left,
                right = insets.right
            )
            findViewById<View>(ModuleR.id.container).updatePadding(
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            windowInsets
        }

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