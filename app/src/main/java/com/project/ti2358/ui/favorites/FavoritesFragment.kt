package com.project.ti2358.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.ti2358.R
import com.project.ti2358.data.manager.ChartManager
import com.project.ti2358.data.manager.OrderbookManager
import com.project.ti2358.data.manager.Stock
import com.project.ti2358.data.manager.StrategyFavorites
import com.project.ti2358.databinding.FragmentFavoritesBinding
import com.project.ti2358.databinding.FragmentFavoritesItemBinding
import com.project.ti2358.service.*
import kotlinx.android.synthetic.main.fragment_favorites.*
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {
    val orderbookManager: OrderbookManager by inject()
    val chartManager: ChartManager by inject()
    val strategyFavorites: StrategyFavorites by inject()

    private var fragmentFavoritesBinding: FragmentFavoritesBinding? = null

    var adapterList: ItemFavoritesRecyclerViewAdapter = ItemFavoritesRecyclerViewAdapter(emptyList())
    lateinit var stocks: MutableList<Stock>

    override fun onDestroy() {
        fragmentFavoritesBinding = null
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentFavoritesBinding.bind(view)
        fragmentFavoritesBinding = binding

        binding.list.addItemDecoration(DividerItemDecoration(list.context, DividerItemDecoration.VERTICAL))
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapterList

        binding.updateButton.setOnClickListener {
            updateData()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                processText(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                processText(newText)
                return false
            }

            fun processText(text: String) {
                updateData()

                stocks = Utils.search(stocks, text)
                adapterList.setData(stocks)
            }
        })

        binding.searchView.setOnCloseListener {
            updateData()
            false
        }

        updateData()
    }

    private fun updateData() {
        stocks = strategyFavorites.process()
        stocks = strategyFavorites.resort()
        adapterList.setData(stocks)

        updateTitle()
    }

    private fun updateTitle() {
        val act = requireActivity() as AppCompatActivity
        act.supportActionBar?.title = "Избранные (${StrategyFavorites.stocksSelected.size} шт.)"
    }

    inner class ItemFavoritesRecyclerViewAdapter(private var values: List<Stock>) : RecyclerView.Adapter<ItemFavoritesRecyclerViewAdapter.ViewHolder>() {
        fun setData(newValues: List<Stock>) {
            values = newValues
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(FragmentFavoritesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(position)
        override fun getItemCount(): Int = values.size

        inner class ViewHolder(private val binding: FragmentFavoritesItemBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(index: Int) {
                val stock = values[index]
                with(binding) {
                    chooseView.setOnCheckedChangeListener(null)
                    chooseView.isChecked = strategyFavorites.isSelected(stock)

                    tickerView.text = "${index + 1}) ${stock.getTickerLove()}"
                    priceView.text = "${stock.getPrice2359String()} ➡ ${stock.getPriceString()}"

                    priceChangeAbsoluteView.text = stock.changePrice2359DayAbsolute.toMoney(stock)
                    priceChangePercentView.text = stock.changePrice2359DayPercent.toPercent()

                    priceChangeAbsoluteView.setTextColor(Utils.getColorForValue(stock.changePrice2359DayAbsolute))
                    priceChangePercentView.setTextColor(Utils.getColorForValue(stock.changePrice2359DayAbsolute))

                    chooseView.setOnCheckedChangeListener { _, checked ->
                        strategyFavorites.setSelected(stock, checked)
                        updateTitle()
                    }

                    itemView.setOnClickListener {
                        Utils.openTinkoffForTicker(requireContext(), stock.ticker)
                    }

                    orderbookButton.setOnClickListener {
                        orderbookManager.start(stock)
                        orderbookButton.findNavController().navigate(R.id.action_nav_favorites_to_nav_orderbook)
                    }

                    chartButton.setOnClickListener {
                        chartManager.start(stock)
                        chartButton.findNavController().navigate(R.id.action_nav_favorites_to_nav_chart)
                    }

                    itemView.setBackgroundColor(Utils.getColorForIndex(index))
                }
            }
        }
    }
}