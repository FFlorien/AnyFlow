package be.florien.anyflow.feature.alarms.add

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentAddAlarmBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.feature.alarms.ConfirmAlarmMenuHolder
import be.florien.anyflow.common.ui.menu.MenuHolder
import kotlinx.coroutines.launch

class AddAlarmFragment : BaseFragment() {
    override fun getTitle() = getString(R.string.alarm_add)

    private lateinit var binding: FragmentAddAlarmBinding
    private lateinit var viewModel: AddAlarmViewModel
    private lateinit var confirmMenuHolder: MenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[AddAlarmViewModel::class.java]
        anyFlowApp.serverComponent?.inject(viewModel)
        confirmMenuHolder = ConfirmAlarmMenuHolder {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager: AlarmManager = requireContext().getSystemService()
                    ?: return@ConfirmAlarmMenuHolder
                when {
                    alarmManager.canScheduleExactAlarms() -> setAlarm()
                    else -> startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) //todo popup to ward the user
                }
            } else {
                setAlarm()
            }
        }
        (requireActivity() as AlarmActivity).menuCoordinator.addMenuHolder(confirmMenuHolder)
        requireActivity().invalidateOptionsMenu()
        confirmMenuHolder.isVisible = true
    }

    private fun setAlarm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addAlarm()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddAlarmBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.time.setIs24HourView(true)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmPermissionReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                        setAlarm()
                    }
                }
            }

            val filter = IntentFilter(
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )
            registerReceiver(
                requireContext(),
                alarmPermissionReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        confirmMenuHolder.isVisible = false
        (requireActivity() as AlarmActivity).menuCoordinator.removeMenuHolder(confirmMenuHolder)
    }
}