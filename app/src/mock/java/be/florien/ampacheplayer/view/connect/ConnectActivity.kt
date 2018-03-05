package be.florien.ampacheplayer.view.connect

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.api.model.Account
import be.florien.ampacheplayer.api.model.AccountList
import be.florien.ampacheplayer.databinding.ItemAccountBinding
import org.simpleframework.xml.core.Persister
import java.io.InputStreamReader


/**
 * Created by florien on 4/03/18.
 */
class ConnectActivity : ConnectActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reader = InputStreamReader(assets.open("mock_account.xml"))
        val osd = Persister().read(AccountList::class.java, reader)
        binding.extra.mockAccount.layoutManager = LinearLayoutManager(this)
        binding.extra.mockAccount.adapter = AccountAdapter(osd.accounts)
    }

    inner class AccountAdapter(val accounts: List<Account>) : RecyclerView.Adapter<AccountViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder = AccountViewHolder(parent)

        override fun getItemCount(): Int = accounts.size

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            holder.bindAccount(accounts[position])
        }

    }

    inner class AccountViewHolder(
            parent: ViewGroup,
            private val binding: ItemAccountBinding
            = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_account, parent, false))
        : RecyclerView.ViewHolder(binding.root) {

        private lateinit var boundAccount: Account

        fun bindAccount(account: Account) {
            boundAccount = account
            binding.accountName = account.name
            itemView.setOnClickListener {
                vm.server = account.server
                vm.username = account.user
                vm.password = account.password
                vm.connect()
            }
        }
    }
}