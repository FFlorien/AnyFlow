package be.florien.ampacheplayer.api

import be.florien.ampacheplayer.api.model.AmpacheAuthentication
import be.florien.ampacheplayer.persistence.AuthManager
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by florien on 17/01/18.
 */
class AmpacheConnectionDebug
@Inject constructor() : AmpacheConnection() {

    override fun authenticate(user: String, password: String): Observable<AmpacheAuthentication> {
        if (user == "mock") {
            
        }
        return super.authenticate(user, password)
    }
}