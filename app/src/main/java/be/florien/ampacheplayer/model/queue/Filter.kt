package be.florien.ampacheplayer.model.queue

import io.realm.RealmObject
import io.realm.RealmQuery

/**
 * Created by florien on 2/07/17.
 */
class Filter<RT : RealmObject, PT>(
        var filterFunction: RealmQuery<RT>.(String, PT) -> RealmQuery<RT>,
        var fieldName: String,
        var argument: PT,
        var subFilter: List<Filter<RT, Any>> = mutableListOf())