package be.florien.anyflow.feature.player.info.song.quickActions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.ActivityQuickActionsBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.connect.ConnectActivity
import be.florien.anyflow.feature.player.info.song.SongInfoActions
import be.florien.anyflow.feature.player.info.song.SongInfoFragment
import be.florien.anyflow.injection.AnyFlowViewModelFactory
import be.florien.anyflow.injection.QuickActionsComponent
import be.florien.anyflow.injection.ViewModelFactoryHolder
import javax.inject.Inject

class QuickActionsActivity : AppCompatActivity(), ViewModelFactoryHolder {

    private lateinit var binding: ActivityQuickActionsBinding
    private lateinit var activityComponent: QuickActionsComponent
    private lateinit var viewModel: QuickActionsViewModel

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    private val fakeComponent = object : QuickActionsComponent {
        override fun inject(quickActionsActivity: QuickActionsActivity) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = anyFlowApp.userComponent
            ?.quickActionsComponentBuilder()
            ?.build()
        activityComponent = if (component != null) {
            component
        } else {
            startActivity(ConnectActivity::class)
            finish()
            fakeComponent
        }
        super.onCreate(savedInstanceState)

        if (activityComponent == fakeComponent) {
            return
        }
        activityComponent.inject(this)

        viewModel =
            ViewModelProvider(this, viewModelFactory)[QuickActionsViewModel::class.java]
                .apply {
                    val width = getDisplayWidth()
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_actions)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        initToolbar()
        val fragment =
            supportFragmentManager.findFragmentByTag(SongInfoFragment::class.java.simpleName)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container_view, SongInfoFragment(
                        SongInfo(
                            SongInfoActions.DUMMY_SONG_ID,
                            getString(R.string.info_title),
                            getString(R.string.info_artist),
                            0L,
                            getString(R.string.info_album),
                            0L,
                            getString(R.string.info_album_artist),
                            0L,
                            listOf(getString(R.string.info_genre)),
                            listOf(0L),
                            1,
                            120,
                            2000,
                            null
                        )
                    ), SongInfoFragment::class.java.simpleName
                )
                .commit()
        }
    }

    override fun getFactory() = viewModelFactory

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
        }
    }
}