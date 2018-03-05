package be.florien.ampacheplayer.api.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * Created by florien on 4/03/18.
 */
@Root(name = "root")
class Account {
    @field:Element(name = "server", required = false)
    var server: String = ""
    @field:Element(name = "name", required = false)
    var name: String = ""
    @field:Element(name = "user", required = false)
    var user: String = ""
    @field:Element(name = "password", required = false)
    var password: String = ""
}

@Root(name = "accounts")
class AccountList {
    @field:ElementList(inline = true, required = false)
    var accounts: List<Account> = mutableListOf()
}