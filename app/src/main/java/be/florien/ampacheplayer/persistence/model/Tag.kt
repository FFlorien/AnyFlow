package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import be.florien.ampacheplayer.api.model.AmpacheTag
import be.florien.ampacheplayer.api.model.AmpacheTagName
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to tags
 */
@Entity
open class Tag {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""

    constructor() : super()

    constructor(fromServer: AmpacheTag) : super() {
        id = fromServer.id
        name = fromServer.name
    }

    constructor(fromServer: AmpacheTagName) : super() {
        id = fromServer.id
        name = fromServer.value
    }
}