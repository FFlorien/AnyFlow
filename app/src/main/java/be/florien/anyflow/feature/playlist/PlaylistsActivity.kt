package be.florien.anyflow.feature.playlist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import be.florien.anyflow.R
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.playlist.list.PlaylistListFragment

class PlaylistsActivity : AppCompatActivity() {
    lateinit var toolbar: Toolbar

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