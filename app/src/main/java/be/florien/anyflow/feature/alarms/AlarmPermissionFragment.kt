package be.florien.anyflow.feature.alarms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentAlarmPermissionBinding
import be.florien.anyflow.common.ui.BaseFragment

class AlarmPermissionFragment : BaseFragment() {

    override fun getTitle() = getString(R.string.alarm_permission)

    private lateinit var binding: FragmentAlarmPermissionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.permissionButton.setOnClickListener {
            /*if(Build.VERSION.SDK_INT >= 31) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }*/
        }
    }
}