package be.florien.ampacheplayer.view.connect

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.api.model.Account
import be.florien.ampacheplayer.api.model.AccountList
import be.florien.ampacheplayer.databinding.ItemAccountBinding
import org.simpleframework.xml.core.Persister
import java.io.File
import java.io.FileReader


/**
 * Created by florien on 4/03/18.
 */
class ConnectActivity : ConnectActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/mock_accounts.xml")
        val serializer = Persister()
        val reader = FileReader(file)
        val osd = serializer.read(AccountList::class.java, reader)


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

        fun bindAccount(account: Account) {
            binding.accountName = account.name
        }
    }

}