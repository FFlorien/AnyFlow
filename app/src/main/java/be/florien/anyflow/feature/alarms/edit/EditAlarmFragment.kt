package be.florien.anyflow.feature.alarms.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Alarm
import be.florien.anyflow.databinding.FragmentEditAlarmBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.feature.menu.ConfirmAlarmMenuHolder
import be.florien.anyflow.feature.menu.DeleteAlarmMenuHolder
import be.florien.anyflow.feature.menu.MenuHolder
import kotlinx.coroutines.launch

class EditAlarmFragment(var alarm: Alarm = Alarm(0L, 0, 0, false, listOf(), false)) : BaseFragment() {
    companion object {
        const val ALARM_TO_EDIT = "alarmToEdit"
    }

    override fun getTitle() = getString(R.string.alarm_edit)

    private lateinit var binding: FragmentEditAlarmBinding
    private lateinit var viewModel: EditAlarmViewModel
    private lateinit var confirmMenuHolder: MenuHolder
    private lateinit var deleteMenuHolder: MenuHolder

    init {
        arguments?.let {
            alarm = it.getParcelable(ALARM_TO_EDIT) ?: Alarm(0L, 0, 0, false, listOf(false, false, false, false, false, false, false), false)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putParcelable(ALARM_TO_EDIT, alarm)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(EditAlarmViewModel::class.java)
        viewModel.alarm = alarm
        anyFlowApp.applicationComponent.inject(viewModel)
        confirmMenuHolder = ConfirmAlarmMenuHolder {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.editAlarm()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        deleteMenuHolder = DeleteAlarmMenuHolder {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.deleteAlarm()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        (requireActivity() as AlarmActivity).menuCoordinator.addMenuHolder(confirmMenuHolder)
        (requireActivity() as AlarmActivity).menuCoordinator.addMenuHolder(deleteMenuHolder)
        requireActivity().invalidateOptionsMenu()
        confirmMenuHolder.isVisible = true
        deleteMenuHolder.isVisible = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditAlarmBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.time.setIs24HourView(true)
    }

    override fun onDetach() {
        super.onDetach()
        deleteMenuHolder.isVisible = false
        confirmMenuHolder.isVisible = false
        (requireActivity() as AlarmActivity).menuCoordinator.removeMenuHolder(deleteMenuHolder)
        (requireActivity() as AlarmActivity).menuCoordinator.removeMenuHolder(confirmMenuHolder)
    }
}