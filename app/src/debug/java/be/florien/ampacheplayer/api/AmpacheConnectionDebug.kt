package be.florien.ampacheplayer.api

import android.content.Context
import be.florien.ampacheplayer.user.AuthManager
import javax.inject.Inject

/**
 * Created by florien on 17/01/18.
 */
class AmpacheConnectionDebug
@Inject constructor(authManager: AuthManager, context: Context) : AmpacheConnection(authManager, context) {

//    override fun authenticate(user: String, password: String): Observable<AmpacheAuthentication> {
//        if (user == "mock") {
//
//        }
//        return super.authenticate(user, password)
//    }
}