package be.florien.ampacheplayer.api.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList

/**
 * Created by florien on 4/03/18.
 */
class Account {
    @field:Element(name = "name", required = false)
    var name: String = ""
    @field:Element(name = "user", required = false)
    var user: String = ""
    @field:Element(name = "password", required = false)
    var password: String = ""
    @field:Element(name = "time", required = false)
    var time: String = ""
}

class AccountList {
    @field:ElementList(inline = true, required = false)
    var accounts: List<Account> = mutableListOf()
}