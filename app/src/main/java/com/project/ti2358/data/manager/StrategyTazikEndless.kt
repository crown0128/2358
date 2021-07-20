package com.project.ti2358.data.manager

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.project.ti2358.R
import com.project.ti2358.TheApplication
import com.project.ti2358.data.common.BrokerType
import com.project.ti2358.data.tinkoff.model.Candle
import com.project.ti2358.data.tinkoff.model.Currency
import com.project.ti2358.service.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.Collections.synchronizedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

@KoinApiExtension
class StrategyTazikEndless : KoinComponent {
    private val stockManager: StockManager by inject()
    private val tinkoffPortfolioManager: TinkoffPortfolioManager by inject()
    private val alorPortfolioManager: AlorPortfolioManager by inject()
    private val strategySpeaker: StrategySpeaker by inject()
    private val strategyTelegram: StrategyTelegram by inject()
    private val strategyBlacklist: StrategyBlacklist by inject()

    var stocks: MutableList<Stock> = mutableListOf()
    var stocksSelected: MutableList<Stock> = mutableListOf()

    var stocksToPurchase: MutableList<StockPurchase> = mutableListOf()
    var stocksToClonePurchase: MutableList<StockPurchase> = mutableListOf()

    var stocksPurchaseInProcess: MutableMap<StockPurchase, Job> = ConcurrentHashMap()

    var basicPercentLimitPriceChange: Double = 0.0
    var started: Boolean = false
    var scheduledTimeStart: Calendar? = null
    var scheduledTimeEnd: Calendar? = null

    var jobResetPrice: Job? = null

    var currentSort: Sorting = Sorting.DESCENDING
    var currentPurchaseSort: Sorting = Sorting.DESCENDING

    companion object {
        const val PercentLimitChangeDelta = 0.05
    }

    suspend fun process(numberSet: Int) = withContext(StockManager.stockContext) {
        val all = stockManager.getWhiteStocks()
        val min = SettingsManager.getCommonPriceMin()
        val max = SettingsManager.getCommonPriceMax()

        stocks = all.filter { (it.getPriceNow() > min && it.getPriceNow() < max) || it.getPriceNow() == 0.0 }.toMutableList()
        stocks.sortBy { it.changePrice2300DayPercent }

        stocks.removeAll { it.instrument.currency == Currency.USD && it.getPrice2300() == 0.0 }

        loadSelectedStocks(numberSet)
    }

    private suspend fun loadSelectedStocks(numberSet: Int) = withContext(StockManager.stockContext) {
        stocksSelected.clear()

        val setList: List<String> = when (numberSet) {
            1 -> SettingsManager.getTazikEndlessSet1()
            2 -> SettingsManager.getTazikEndlessSet2()
            3 -> SettingsManager.getTazikEndlessSet3()
            4 -> SettingsManager.getLoveSet()
            else -> emptyList()
        }
        stocksSelected = stocks.filter { it.ticker in setList }.toMutableList()
    }

    private suspend fun saveSelectedStocks(numberSet: Int) = withContext(StockManager.stockContext) {
        val setList = stocksSelected.map { it.ticker }.toMutableList()

        val preferences = PreferenceManager.getDefaultSharedPreferences(TheApplication.application.applicationContext)
        val editor: SharedPreferences.Editor = preferences.edit()

        val key = when (numberSet) {
            1 -> TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set)
            2 -> TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set_2)
            3 -> TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set_3)
            4 -> TheApplication.application.applicationContext.getString(R.string.setting_key_love_set)
            else -> ""
        }

        if (key != "") {
            editor.putString(key, setList.joinToString(separator = " "))
            editor.apply()
        }
    }

    suspend fun resort(): MutableList<Stock> = withContext(StockManager.stockContext) {
        currentSort = if (currentSort == Sorting.DESCENDING) Sorting.ASCENDING else Sorting.DESCENDING
        stocks.sortBy {
            val sign = if (currentSort == Sorting.ASCENDING) 1 else -1
            val multiplier = if (it in stocksSelected) 100 else 1
            val change = if (it.changePrice2300DayPercent.isInfinite()) 100.0 else it.changePrice2300DayPercent
            val final = change * sign - multiplier
            if (final.isNaN()) 0.0 else final
        }
        return@withContext stocks
    }

    suspend fun setSelected(stock: Stock, value: Boolean, numberSet: Int) = withContext(StockManager.stockContext) {
        if (value) {
            if (stock !in stocksSelected)
                stocksSelected.add(stock)
        } else {
            stocksSelected.remove(stock)
        }
        stocksSelected.sortBy { it.changePrice2300DayPercent }

        saveSelectedStocks(numberSet)
    }

    fun isSelected(stock: Stock): Boolean {
        return stock in stocksSelected
    }

    suspend fun getPurchaseStock(): MutableList<StockPurchase> = withContext(StockManager.stockContext) {
        if (started) return@withContext stocksToPurchase
        val percent = SettingsManager.getTazikEndlessChangePercent()

        val totalMoneyTinkoff: Double = SettingsManager.getTazikEndlessPurchaseVolume().toDouble()
        val onePieceTinkoff: Double = totalMoneyTinkoff / SettingsManager.getTazikEndlessPurchaseParts()

        val totalMoneyAlor: Double = SettingsManager.getTazikEndlessPurchaseVolume().toDouble() * SettingsManager.getAlorMultiplierMoney()
        val onePieceAlor: Double = totalMoneyAlor / SettingsManager.getTazikEndlessPurchaseParts()

        val purchases: MutableList<StockPurchase> = mutableListOf()
        for (stock in stocksSelected) {
            if (SettingsManager.getBrokerTinkoff()) {
                val purchase = StockPurchase(stock, BrokerType.TINKOFF)
                purchases.add(purchase)
            }

            if (SettingsManager.getBrokerAlor()) {
                val purchase = StockPurchase(stock, BrokerType.ALOR)
                purchases.add(purchase)
            }
        }

        purchases.forEach {
            // сбросить все проценты без сохранения
            it.percentLimitPriceChange = percent

            // посчитать количество лотов на каждую бумагу/брокера
            val part = when (it.broker) {
                BrokerType.TINKOFF -> if (it.stock.instrument.currency == Currency.RUB) onePieceTinkoff * Utils.getUSDRUB() else onePieceTinkoff
                BrokerType.ALOR -> if (it.stock.instrument.currency == Currency.RUB) onePieceAlor * Utils.getUSDRUB() else onePieceAlor
                else -> 0.0
            }

            if (it.stock.getPriceNow() != 0.0) {
                it.lots = (part / it.stock.getPriceNow()).roundToInt()
            }
            it.updateAbsolutePrice()
            it.status = PurchaseStatus.WAITING
        }
        stocksToPurchase = synchronizedList(purchases)

        // удалить все бумаги, которые уже есть в портфеле, чтобы избежать коллизий
        if (SettingsManager.getTazikEndlessExcludeDepo()) {
            stocksToPurchase.removeAll { p -> tinkoffPortfolioManager.portfolioPositionTinkoffs.any { it.ticker == p.ticker && p.broker == BrokerType.TINKOFF } }
            stocksToPurchase.removeAll { p -> alorPortfolioManager.portfolioPositions.any { it.symbol == p.ticker && p.broker == BrokerType.ALOR } }
        }

        // удалить все бумаги, у которых 0 лотов = не хватает на покупку одной части
        stocksToPurchase.removeAll { it.lots == 0 || it.lots > 9999999 }

        // удалить все бумаги, у которых недавно или скоро отчёты
        if (SettingsManager.getTazikEndlessExcludeReports()) {
            stocksToPurchase.removeAll { it.stock.report != null }
        }

        // удалить все бумаги, у которых скоро дивы
        if (SettingsManager.getTazikEndlessExcludeDivs()) {
            stocksToPurchase.removeAll { it.stock.dividend != null }
        }

        // удалить все бумаги, у которых скоро FDA фаза
        if (SettingsManager.getTazikEndlessExcludeFDA()) {
            stocksToPurchase.removeAll { it.stock.fda != null }
        }

        // удалить все бумаги из чёрного списка
        val blacklist = strategyBlacklist.getBlacklistStocks()
        stocksToPurchase.removeAll { it.ticker in blacklist.map { stock -> stock.ticker } }

        stocksToClonePurchase = stocksToPurchase.toMutableList()

        return@withContext stocksToPurchase
    }

    fun getNotificationTitle(): String = runBlocking(StockManager.stockContext) {
        if (started) {
            if (scheduledTimeEnd != null) {
                val now = Calendar.getInstance(TimeZone.getDefault())
                val current = scheduledTimeEnd?.timeInMillis ?: 0
                val scheduleDelay = current - now.timeInMillis

                val allSeconds = scheduleDelay / 1000
                val hours = allSeconds / 3600
                val minutes = (allSeconds - hours * 3600) / 60
                val seconds = allSeconds % 60

                fixPrice()
                if (hours + minutes + seconds <= 0) {
                    GlobalScope.launch(Dispatchers.Main) {
                        stopStrategyCommand()
                    }
                }

                return@runBlocking "Работает 🛁! Финиш через %02d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                return@runBlocking "Работает бесконечный 🛁!"
            }
        }

        if (scheduledTimeStart != null) {
            val now = Calendar.getInstance(TimeZone.getDefault())
            val current = scheduledTimeStart?.timeInMillis ?: 0
            val scheduleDelay = current - now.timeInMillis

            val allSeconds = scheduleDelay / 1000
            val hours = allSeconds / 3600
            val minutes = (allSeconds - hours * 3600) / 60
            val seconds = allSeconds % 60

            fixPrice()
            if (hours + minutes + seconds <= 0) {
                startStrategy(true)
            }

            return@runBlocking "Старт 🛁 через %02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            return@runBlocking "Бесконечный 🛁 приостановлен"
        }
    }

    fun getTotalPurchaseString(): String {
        val volume = SettingsManager.getTazikEndlessPurchaseVolume().toDouble()
        val p = SettingsManager.getTazikEndlessPurchaseParts()
        val volumeShares = SettingsManager.getTazikEndlessMinVolume()
        return String.format(
            "%d из %d по %.2f$, просадка %.2f / %.2f / %.2f / %d",
            stocksPurchaseInProcess.size,
            p,
            volume / p,
            basicPercentLimitPriceChange,
            SettingsManager.getTazikEndlessTakeProfit(),
            SettingsManager.getTazikEndlessApproximationFactor(),
            volumeShares
        )
    }

    fun getNotificationTextShort(): String {
        val price = getTotalPurchaseString()
        var tickers = ""
        for (stock in stocksToPurchase) {
            tickers += "${stock.ticker} "
        }
        return "$price:\n$tickers"
    }

    fun getSortedPurchases(): List<StockPurchase> {
        currentPurchaseSort = if (currentPurchaseSort == Sorting.DESCENDING) Sorting.ASCENDING else Sorting.DESCENDING

        val local = stocksToPurchase.toMutableList()
        local.removeAll { it.tazikEndlessPrice == 0.0 }

        if (currentPurchaseSort == Sorting.ASCENDING) {
            local.sortBy { it.stock.getPriceNow() / it.tazikEndlessPrice * 100 - 100 }
        } else {
            local.sortByDescending { it.stock.getPriceNow() / it.tazikEndlessPrice * 100 - 100 }
        }

        return local
    }

    fun getNotificationTextLong(): String {
        val stocks = stocksToPurchase.map {
            Pair(it.stock.getPriceNow(), it)
        }.sortedBy {
            it.first / it.second.tazikEndlessPrice * 100 - 100
        }

        var tickers = ""
        for (pair in stocks) {
            val purchase = pair.second
            val priceNow = pair.first

            val change = (100 * priceNow) / purchase.tazikEndlessPrice - 100
            if (change >= -0.01 && purchase.status == PurchaseStatus.WAITING && stocksToPurchase.size > 5) continue

            var vol = 0
            if (purchase.stock.minuteCandles.isNotEmpty()) {
                vol = purchase.stock.minuteCandles.last().volume
            }
            tickers += "${purchase.ticker} ${purchase.percentLimitPriceChange.toPercent()} = " +
                    "${purchase.tazikEndlessPrice.toMoney(purchase.stock)} ➡ ${priceNow.toMoney(purchase.stock)} = " +
                    "${change.toPercent()} ${purchase.getStatusString()} v=${vol}\n"
        }
        if (tickers == "") tickers = "отображаются только просадки ⏳"

        return tickers
    }

    private fun fixPrice() {
        // зафикировать цену, чтобы change считать от неё
        for (purchase in stocksToClonePurchase) {
            purchase.tazikEndlessPrice = purchase.stock.getPriceNow()
        }
    }

    suspend fun restartStrategy(newPercent: Double = 0.0, profit: Double = 0.0) = withContext(StockManager.stockContext) {
        if (started) stopStrategy()

        if (newPercent != 0.0) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(TheApplication.application.applicationContext)
            val editor: SharedPreferences.Editor = preferences.edit()
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_min_percent_to_buy)
            editor.putString(key, "%.2f".format(locale = Locale.US, newPercent))
            editor.apply()
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(TheApplication.application.applicationContext)
        val editor: SharedPreferences.Editor = preferences.edit()
        val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_take_profit)
        editor.putString(key, "%.2f".format(locale = Locale.US, profit))
        editor.apply()

        process(1)
        getPurchaseStock()
        delay(500)

        Utils.startService(TheApplication.application.applicationContext, StrategyTazikEndlessService::class.java)
        startStrategy(false)
    }

    suspend fun stopStrategyCommand() = withContext(StockManager.stockContext) {
        Utils.stopService(TheApplication.application.applicationContext, StrategyTazikEndlessService::class.java)
    }

    fun prepareStrategy(scheduled : Boolean, timeFromTo: Pair<String, String>) = runBlocking (StockManager.stockContext) {
        basicPercentLimitPriceChange = SettingsManager.getTazikEndlessChangePercent()

        if (!scheduled) {
            scheduledTimeStart = null
            scheduledTimeEnd = null

            startStrategy(scheduled)
            return@runBlocking
        }

        started = false
        val differenceHours: Int = Utils.getTimeDiffBetweenMSK()

        if (timeFromTo.first != "") { // старт таза
            val dayTimeStart = timeFromTo.first.split(":").toTypedArray()
            if (dayTimeStart.size < 3) {
                GlobalScope.launch(Dispatchers.Main) {
                    Utils.showToastAlert("Неверный формат времени старта $dayTimeStart")
                }
                return@runBlocking
            }

            val hours = Integer.parseInt(dayTimeStart[0])
            val minutes = Integer.parseInt(dayTimeStart[1])
            val seconds = Integer.parseInt(dayTimeStart[2])

            scheduledTimeStart = Calendar.getInstance(TimeZone.getDefault())
            scheduledTimeStart?.let {
                it.add(Calendar.HOUR_OF_DAY, -differenceHours)
                it.set(Calendar.HOUR_OF_DAY, hours)
                it.set(Calendar.MINUTE, minutes)
                it.set(Calendar.SECOND, seconds)
                it.add(Calendar.HOUR_OF_DAY, differenceHours)

                val now = Calendar.getInstance(TimeZone.getDefault())
                val scheduleDelay = it.timeInMillis - now.timeInMillis
                if (scheduleDelay < 0) {
                    GlobalScope.launch(Dispatchers.Main) {
                        Utils.showToastAlert("Ошибка! Отрицательное время!? втф = $scheduleDelay")
                    }
                }
            }
        }

        if (timeFromTo.second != "") { // старт таза
            val dayTimeEnd = timeFromTo.second.split(":").toTypedArray()
            if (dayTimeEnd.size < 3) {
                GlobalScope.launch(Dispatchers.Main) {
                    Utils.showToastAlert("Неверный формат времени финиша $dayTimeEnd")
                }
                return@runBlocking
            }

            val hours = Integer.parseInt(dayTimeEnd[0])
            val minutes = Integer.parseInt(dayTimeEnd[1])
            val seconds = Integer.parseInt(dayTimeEnd[2])

            scheduledTimeEnd = Calendar.getInstance(TimeZone.getDefault())
            scheduledTimeEnd?.let {
                it.add(Calendar.HOUR_OF_DAY, -differenceHours)
                it.set(Calendar.HOUR_OF_DAY, hours)
                it.set(Calendar.MINUTE, minutes)
                it.set(Calendar.SECOND, seconds)
                it.add(Calendar.HOUR_OF_DAY, differenceHours)

                val now = Calendar.getInstance(TimeZone.getDefault())
                val scheduleDelay = it.timeInMillis - now.timeInMillis
                if (scheduleDelay < 0) {
                    GlobalScope.launch(Dispatchers.Main) {
                        Utils.showToastAlert("Ошибка! Отрицательное время!? втф = $scheduleDelay")
                    }
                }
            }
        }
    }

    private fun clearJobs() {
        stocksPurchaseInProcess.forEach {
            try {
                if (it.value.isActive) {
                    it.value.cancel()
                }
            } catch (e: Exception) {

            }
        }
        stocksPurchaseInProcess.clear()
    }

    suspend fun startStrategy(scheduled: Boolean) = withContext(StockManager.stockContext) {
        scheduledTimeStart = null
        basicPercentLimitPriceChange = SettingsManager.getTazikEndlessChangePercent()

        if (scheduled) {
            GlobalScope.launch(Dispatchers.Main) {
                stockManager.reloadClosePrices()

                // костыль!
                started = false
                fixPrice()
                started = true
            }
        } else {
            fixPrice()
        }

        strategyBlacklist.process(stockManager.stocksStream)
        clearJobs()
        started = true

        jobResetPrice?.cancel()
        jobResetPrice = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                val seconds = SettingsManager.getTazikEndlessResetIntervalSeconds().toLong()
                delay(1000 * seconds)
                fixPrice()
            }
        }

        strategyTelegram.sendTazikEndlessStart(true)
    }

    fun stopStrategy() {
        scheduledTimeEnd = null

        started = false
        clearJobs()

        jobResetPrice?.cancel()
        strategyTelegram.sendTazikEndlessStart(false)
    }

    fun addBasicPercentLimitPriceChange(sign: Int) = runBlocking (StockManager.stockContext) {
        basicPercentLimitPriceChange += sign * PercentLimitChangeDelta

        val preferences = PreferenceManager.getDefaultSharedPreferences(TheApplication.application.applicationContext)
        val editor: SharedPreferences.Editor = preferences.edit()
        val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_min_percent_to_buy)
        editor.putString(key, "%.2f".format(locale = Locale.US, basicPercentLimitPriceChange))
        editor.apply()

        for (purchase in stocksToPurchase) {
            purchase.percentLimitPriceChange += sign * PercentLimitChangeDelta
            if (purchase.stock.minuteCandles.isNotEmpty()) {
                processStrategy(purchase.stock, purchase.stock.minuteCandles.last())
            }
        }
    }

    private fun isAllowToBuy(purchase: StockPurchase, change: Double, volume: Int): Boolean {
        if (purchase.tazikEndlessPrice == 0.0 ||                    // стартовая цена нулевая = не загрузились цены
            abs(change) > 50 ||                                     // конечная цена нулевая или просто огромная просадка
            change > 0 ||                                           // изменение положительное
            change > purchase.percentLimitPriceChange ||            // изменение не в пределах наших настроек
            volume < SettingsManager.getTazikEndlessMinVolume() ||  // если объём свечи меньше настроек
            purchase.stock.getTodayVolume() < SettingsManager.getTazikEndlessDayMinVolume() // дневной объём меньше, чем нужно
        ) {
            return false
        }

        val ticker = purchase.ticker

        // лимит на заявки исчерпан у брокера?
        val countBroker = stocksPurchaseInProcess.filter { it.key.broker == purchase.broker }.size
        if (countBroker >= SettingsManager.getTazikEndlessPurchaseParts()) return false

        // проверить, если бумага в депо и усреднение отключено, то запретить тарить
        if (purchase.broker == BrokerType.TINKOFF) {
            if (tinkoffPortfolioManager.portfolioPositionTinkoffs.find { it.ticker == ticker } != null && !SettingsManager.getTazikEndlessAllowAveraging()) {
                return false
            }
        }

        if (purchase.broker == BrokerType.ALOR) {
            if (alorPortfolioManager.portfolioPositions.find { it.symbol == ticker } != null && !SettingsManager.getTazikEndlessAllowAveraging()) {
                return false
            }
        }

        // ещё не брали бумагу в этом брокере?
        if (stocksPurchaseInProcess.filter { it.key.broker == purchase.broker && it.key.stock.ticker == ticker }.isEmpty()) {
            return true
        }

        // разрешить усреднение?
        if (SettingsManager.getTazikEndlessAllowAveraging()) {
            return true
        }

        return false
    }

    fun processUpdate() = runBlocking(StockManager.stockContext) {
        if (!started) return@runBlocking

        // если стратегия стартанула и какие-то корутины уже завершились, то убрать их, чтобы появился доступ для новых покупок
        for (value in stocksPurchaseInProcess) {
            if (!value.value.isActive) {
                val key = value.key
                stocksPurchaseInProcess.remove(key)
            }
        }
    }

    fun processStrategy(stock: Stock, candle: Candle) {
        if (!started) return
        if (stock in strategyBlacklist.getBlacklistStocks()) return

        val ticker = stock.ticker

        // если бумага не в списке скана - игнорируем
        val sorted = stocksToClonePurchase.filter { it.ticker == ticker }
        sorted.forEach { purchase ->
            val change = candle.closingPrice / purchase.tazikEndlessPrice * 100.0 - 100.0
            val volume = candle.volume

            if (isAllowToBuy(purchase, change, volume)) {
                processBuy(purchase, stock, candle)
            }
        }
    }

    private fun processBuy(purchase: StockPurchase, stock: Stock, candle: Candle) {
        // завершение стратегии
        val parts = SettingsManager.getTazikEndlessPurchaseParts()
        val countBroker = stocksPurchaseInProcess.filter { it.key.broker == purchase.broker }.size
        if (countBroker >= parts) { // останавливить стратегию автоматически
            stopStrategy()
            return
        }

        if (purchase.tazikEndlessPrice == 0.0) return

        val change = candle.closingPrice / purchase.tazikEndlessPrice * 100.0 - 100.0

        // ищем цену максимально близкую к просадке
        var delta = abs(change) - abs(purchase.percentLimitPriceChange)

        // 0.80 коэф приближения к нижней точке, в самом низу могут не налить
        delta *= SettingsManager.getTazikEndlessApproximationFactor()

        // корректируем % падения для покупки
        val percent = abs(purchase.percentLimitPriceChange) + delta

        // вычислияем финальную цену лимитки
        var buyPrice = purchase.tazikEndlessPrice - abs(purchase.tazikEndlessPrice / 100.0 * percent)

        // защита от спайков - сколько минут цена была выше цены покупки, начиная с предыдущей
        var minutes = SettingsManager.getTazikEndlessSpikeProtection()
        if (minutes != 0) { // 0 = без защиты
            if (purchase.stock.minuteCandles.size >= minutes) { // не считать спайки на открытии и на старте таза - мало доступных свечей
                for (i in purchase.stock.minuteCandles.indices.reversed()) {

                    // пропустить текущую свечу, по которой у нас просадка
                    if (i == purchase.stock.minuteCandles.size - 1) continue

                    // проверить цены закрытия нескольких предыдущих свечей
                    if (purchase.stock.minuteCandles[i].closingPrice > buyPrice) { // если цена выше, отнимаем счётчик, проверяем дальше
                        minutes--

                        // если несколько свечей подряд с ценой выше, то всё ок - тарим!
                        if (minutes == 0) {
                            break
                        }
                    } else { // был спайк на несколько свечек - тарить опасно!
                        // обновить цену, чтобы не затарить на следующей свече, возможен нож ступенькой
                        purchase.tazikEndlessPrice = candle.closingPrice
                        strategySpeaker.speakTazikSpikeSkip(purchase, change)
                        strategyTelegram.sendTazikSpike(
                            purchase,
                            buyPrice,
                            purchase.tazikEndlessPrice,
                            candle.closingPrice,
                            change,
                            stocksPurchaseInProcess.size,
                            parts
                        )
                        return
                    }
                }
            }
        }

        // проверка на цену закрытия (выше не тарить)
        if (SettingsManager.getTazikEndlessClosePriceProtectionPercent() != 0.0) {
            if (stock.instrument.currency == Currency.USD) {
                val finalPrice = stock.getPrice2300() + stock.getPrice2300() / 100.0 * SettingsManager.getTazikEndlessClosePriceProtectionPercent()
                if (buyPrice >= finalPrice) {
                    return
                }
            } else {
                if (buyPrice >= stock.getPrice1000()) {
                    return
                }
            }
        }

        // вычисляем процент профита после сдвига лимитки ниже
        var finalProfit = SettingsManager.getTazikEndlessTakeProfit()

        // если мы усредняем, то не нужно выставлять ТП, потому что неизвестно какие заявки из усреднения выполнятся и какая будет в итоге средняя
        if (stocksPurchaseInProcess.filter { it.key.broker == purchase.broker && it.key.stock.ticker == purchase.ticker }.isNotEmpty() && SettingsManager.getTazikEndlessAllowAveraging()) {
            finalProfit = 0.0
        }

        buyPrice = Utils.makeNicePrice(buyPrice, stock)
        val job = purchase.buyLimitFromBid(buyPrice, finalProfit, 1, SettingsManager.getTazikEndlessOrderLifeTimeSeconds())
        if (job != null) {
            stocksPurchaseInProcess[purchase] = job

            var sellPrice = buyPrice + buyPrice / 100.0 * finalProfit
            sellPrice = Utils.makeNicePrice(sellPrice, stock)

            strategySpeaker.speakTazik(purchase, change)
            strategyTelegram.sendTazikBuy(purchase, buyPrice, sellPrice, purchase.tazikEndlessPrice, candle.closingPrice, change, countBroker, parts)
            purchase.tazikEndlessPrice = candle.closingPrice
        }
    }
}