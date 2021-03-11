package com.project.ti2358.ui.portfolio

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.ti2358.R
import com.project.ti2358.TheApplication
import com.project.ti2358.data.manager.DepositManager
import com.project.ti2358.data.model.dto.PortfolioPosition
import com.project.ti2358.data.service.ThirdPartyService
import com.project.ti2358.service.Utils
import com.project.ti2358.service.toDollar
import com.project.ti2358.service.toPercent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.inject
import java.lang.Exception

@KoinApiExtension
class PortfolioFragment : Fragment() {

    private val thirdPartyService: ThirdPartyService by inject()
    val depositManager: DepositManager by inject()
    var adapterList: ItemPortfolioRecyclerViewAdapter = ItemPortfolioRecyclerViewAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_portfolio_item_list, container, false)
        val list = view.findViewById<RecyclerView>(R.id.list)

        list.addItemDecoration(
            DividerItemDecoration(
                list.context,
                DividerItemDecoration.VERTICAL
            )
        )

        if (list is RecyclerView) {
            with(list) {
                layoutManager = LinearLayoutManager(context)
                adapter = adapterList
            }
        }

        val buttonUpdate = view.findViewById<Button>(R.id.buttonUpdate)
        buttonUpdate.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                updateData()
            }
        }

        val buttonOrder = view.findViewById<Button>(R.id.buttonOrders)
        buttonOrder.setOnClickListener {
            view.findNavController().navigate(R.id.action_nav_portfolio_to_nav_orders)
        }

        GlobalScope.launch(Dispatchers.Main) {
            updateData()

            delay(2000)
            val version = thirdPartyService.githubVersion()
            val currentVersion = try {
                val pInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                version
            }

            if (version != currentVersion) { // показать окно обновления
                Utils.showUpdateAlert(requireContext(), currentVersion, version)
            }
        }
        return view
    }

    fun updateData() {
        GlobalScope.launch(Dispatchers.Main) {
            depositManager.refreshDeposit()
            depositManager.refreshKotleta()
            adapterList.setData(depositManager.portfolioPositions)
            updateTitle()
        }
    }

    private fun updateTitle() {
        val act = requireActivity() as AppCompatActivity

        val percent = depositManager.getPercentBusyInStocks()
        act.supportActionBar?.title = "Депозит $percent%"
    }

    class ItemPortfolioRecyclerViewAdapter(
        private var values: List<PortfolioPosition>
    ) : RecyclerView.Adapter<ItemPortfolioRecyclerViewAdapter.ViewHolder>() {

        fun setData(newValues: List<PortfolioPosition>) {
            values = newValues
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_portfolio_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.tickerView.text = "${position}. ${item.ticker}"

            val avg = item.getAveragePrice()
            holder.volumePiecesView.text = "${item.lots} шт."
            holder.priceView.text = "${avg}"

            val profit = item.getProfitAmount()
            holder.changePriceAbsoluteView.text = "%.2f $".format(profit)

            var totalCash = item.balance * avg
            val percent = item.getProfitPercent()
            holder.changePricePercentView.text = percent.toPercent()

            totalCash += profit
            holder.volumeCashView.text = totalCash.toDollar()

            if (percent < 0) {
                holder.changePriceAbsoluteView.setTextColor(Utils.RED)
                holder.changePricePercentView.setTextColor(Utils.RED)
            } else {
                holder.changePriceAbsoluteView.setTextColor(Utils.GREEN)
                holder.changePricePercentView.setTextColor(Utils.GREEN)
            }
        }

        override fun getItemCount(): Int = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tickerView: TextView = view.findViewById(R.id.stock_item_ticker)
            val priceView: TextView = view.findViewById(R.id.stock_item_price)

            val volumeCashView: TextView = view.findViewById(R.id.stock_item_volume_cash)
            val volumePiecesView: TextView = view.findViewById(R.id.stock_item_volume_pieces)

            val changePriceAbsoluteView: TextView = view.findViewById(R.id.stock_item_price_change_absolute)
            val changePricePercentView: TextView = view.findViewById(R.id.stock_item_price_change_percent)
        }
    }
}