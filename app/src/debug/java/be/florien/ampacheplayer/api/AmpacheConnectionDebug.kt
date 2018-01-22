package be.florien.ampacheplayer.api

import android.content.Context
import be.florien.ampacheplayer.user.AuthPersistence
import javax.inject.Inject

/**
 * Created by florien on 17/01/18.
 */
class AmpacheConnectionDebug
@Inject constructor(authPersistence: AuthPersistence, context: Context) : AmpacheConnection(authPersistence, context) {

//    override fun authenticate(user: String, password: String): Observable<AmpacheAuthentication> {
//        if (user == "mock") {
//
//        }
//        return super.authenticate(user, password)
//    }
}