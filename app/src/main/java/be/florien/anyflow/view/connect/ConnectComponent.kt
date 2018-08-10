package be.florien.anyflow.view.connect

import android.app.Activity
import android.view.View
import be.florien.anyflow.di.ActivityScope
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent()
@ActivityScope
interface ConnectComponent {

    fun inject(connectActivityBase: ConnectActivityBase)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun view(view: View): Builder

        fun build(): ConnectComponent
    }

}
