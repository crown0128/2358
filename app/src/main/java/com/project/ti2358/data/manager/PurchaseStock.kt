package com.project.ti2358.data.manager

import com.project.ti2358.data.model.dto.*
import com.project.ti2358.data.service.*
import com.project.ti2358.service.Utils
import com.project.ti2358.service.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs
import kotlin.math.ceil

enum class OrderStatus {
    NONE,
    ORDER_BUY_PREPARE,
    ORDER_BUY,
    BOUGHT,
    ORDER_SELL_TRAILING,
    ORDER_SELL_PREPARE,
    ORDER_SELL,
    WAITING,
    SOLD,
    CANCELED,
    NOT_FILLED,
    PART_FILLED,

    ERROR_NEED_WATCH,

    WTF_1,
    WTF_2,
    WTF_3,
    WTF_4,
}

@KoinApiExtension
data class PurchaseStock(
    var stock: Stock
) : KoinComponent {
    private val ordersService: OrdersService by inject()
    private val depositManager: DepositManager by inject()
    private val marketService: MarketService by inject()

    lateinit var position: PortfolioPosition
    var percentLimitPriceChange: Double = 0.0         // разница в % с текущей ценой для создания лимитки
    var absoluteLimitPriceChange: Double = 0.0        // если лимитка, то по какой цене

    var lots: Int = 0                                 // сколько штук тарим / продаём
    var status: OrderStatus = OrderStatus.NONE

    var buyMarketOrder: MarketOrder? = null
    var buyLimitOrder: LimitOrder? = null
    var sellLimitOrder: LimitOrder? = null

    var percentProfitSellFrom: Double = 0.0
    var percentProfitSellTo: Double = 0.0

    var trailingTake: Boolean = false
    var trailingTakeActivationPercent: Double = 0.0
    var trailingTakeStopDelta: Double = 0.0

    var currentTrailingTakeProfit: TrailingTakeProfit? = null

    companion object {
        const val DelayFast: Long = 150
        const val DelayMiddle: Long = 400
        const val DelayLong: Long = 2000
    }

    fun getPriceString(): String {
        return "%.1f$".format(stock.getPriceDouble() * lots)
    }

    fun getStatusString(): String =
        when (status) {
            OrderStatus.NONE -> "NONE"
            OrderStatus.WAITING -> "ждём ⏳"
            OrderStatus.ORDER_BUY_PREPARE -> "ордер: до покупки"
            OrderStatus.ORDER_BUY -> "ордер: покупка"
            OrderStatus.BOUGHT -> "куплено! 💸"
            OrderStatus.ORDER_SELL_TRAILING -> "трейлинг стоп 📈"
            OrderStatus.ORDER_SELL_PREPARE -> "ордер: до продажи"
            OrderStatus.ORDER_SELL -> "ордер: продажа 🙋"
            OrderStatus.SOLD -> "продано! 🤑"
            OrderStatus.CANCELED -> "отменена! шок, скринь! 😱"
            OrderStatus.NOT_FILLED -> "не налили 😰"
            OrderStatus.PART_FILLED -> "частично налили, продаём"
            OrderStatus.ERROR_NEED_WATCH -> "ошибка, дальше руками"

            OrderStatus.WTF_1 -> "wtf 1"
            OrderStatus.WTF_2 -> "wtf 2"
            OrderStatus.WTF_3 -> "wtf 3"
            OrderStatus.WTF_4 -> "wtf 4"
        }

    fun getLimitPriceDouble(): Double {
        val price = stock.getPriceDouble() + absoluteLimitPriceChange
        return Utils.makeNicePrice(price)
    }

    fun addPriceLimitPercent(change: Double) {
        percentLimitPriceChange += change
        updateAbsolutePrice()
    }

    fun updateAbsolutePrice() {
        absoluteLimitPriceChange = stock.getPriceDouble() / 100 * percentLimitPriceChange
        absoluteLimitPriceChange = Utils.makeNicePrice(absoluteLimitPriceChange)
    }

    fun addPriceProfit2358Percent(change: Double) {
        percentProfitSellFrom += change
        percentProfitSellTo += change
    }

    fun addPriceProfit2358TrailingTakeProfit(change: Double) {
        trailingTakeActivationPercent += change
        trailingTakeStopDelta += change * 0.4
    }

    fun buyMarket(price: Double) {
        if (lots == 0) return

        val sellPrice = Utils.makeNicePrice(price)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                while (true) {
                    try {
                        status = OrderStatus.ORDER_BUY_PREPARE
                        buyMarketOrder = ordersService.placeMarketOrder(
                            lots,
                            stock.instrument.figi,
                            OperationType.BUY,
                            depositManager.getActiveBrokerAccountId()
                        )
                        status = OrderStatus.ORDER_BUY
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                // проверяем появился ли в портфеле тикер
                var position: PortfolioPosition? = null
                var counter = 50
                while (counter > 0) {
                    depositManager.refreshDeposit()

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position != null && position.lots >= lots) { // куплено!
                        status = OrderStatus.BOUGHT
                        break
                    }

                    delay(DelayLong)
                    counter--
                }

                if (sellPrice == 0.0) return@launch

                // выставить ордер на продажу
                while (true) {
                    try {
                        status = OrderStatus.ORDER_SELL_PREPARE
                        position?.let {
                            sellLimitOrder = ordersService.placeLimitOrder(
                                it.lots,
                                stock.instrument.figi,
                                sellPrice,
                                OperationType.SELL,
                                depositManager.getActiveBrokerAccountId()
                            )
                        }
                        status = OrderStatus.ORDER_SELL
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                while (true) {
                    delay(DelayLong)

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position == null) { // продано!
                        status = OrderStatus.SOLD
                        break
                    }
                }

            } catch (e: Exception) {
                status = OrderStatus.CANCELED
                e.printStackTrace()
            }
        }
    }

    fun buyLimitFromBid(price: Double, profit: Double) {
        if (lots == 0 || price == 0.0 || profit == 0.0) return
        val buyPrice = Utils.makeNicePrice(price)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                while (true) {
                    try {
                        status = OrderStatus.ORDER_BUY_PREPARE
                        buyLimitOrder = ordersService.placeLimitOrder(
                            lots,
                            stock.instrument.figi,
                            buyPrice,
                            OperationType.BUY,
                            depositManager.getActiveBrokerAccountId()
                        )
                        status = OrderStatus.ORDER_BUY
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                val ticker = stock.instrument.ticker
                Utils.showToastAlert("$ticker: ордер по $buyPrice")

                // проверяем появился ли в портфеле тикер
                var position: PortfolioPosition?
                while (true) {
                    try {
                        depositManager.refreshDeposit()
                        depositManager.refreshOrders()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val order = depositManager.getOrderForFigi(stock.instrument.figi, OperationType.BUY)
                    position = depositManager.getPositionForFigi(stock.instrument.figi)

                    // заявка стоит, ничего не куплено
                    if (order != null && position == null) {
                        status = OrderStatus.ORDER_BUY
                        delay(DelayMiddle)
                        continue
                    }

                    if (order == null && position == null) { // заявка отменена, ничего не куплено
                        status = OrderStatus.NOT_FILLED
                        Utils.showToastAlert("$ticker: не налили по $buyPrice")
                        return@launch
                    }

                    if (order == null && position != null) { // заявка отменена или полностью заполнена, продаём всё что куплено
                        status = OrderStatus.BOUGHT
                        Utils.showToastAlert("$ticker: куплено по $buyPrice")

                        var profitPrice = buyPrice + buyPrice / 100.0 * profit
                        profitPrice = Utils.makeNicePrice(profitPrice)

                        // выставить ордер на продажу
                        while (true) {
                            try {
                                position = depositManager.getPositionForFigi(stock.instrument.figi)
                                var lotsLeft = 0
                                if (position != null) lotsLeft = position.lots - position.blocked.toInt()
                                if (lotsLeft == 0) break

                                status = OrderStatus.ORDER_SELL_PREPARE
                                sellLimitOrder = ordersService.placeLimitOrder(
                                    lotsLeft,
                                    stock.instrument.figi,
                                    profitPrice,
                                    OperationType.SELL,
                                    depositManager.getActiveBrokerAccountId()
                                )
                                status = OrderStatus.ORDER_SELL
                                Utils.showToastAlert("$ticker: заявка на продажу по $profitPrice")
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(DelayMiddle)
                        }
                        break
                    }

                    if (order != null && position != null) { // заявка стоит, частично куплено, можно продавать
                        status = OrderStatus.PART_FILLED

                        var profitPrice = buyPrice + buyPrice / 100.0 * profit
                        profitPrice = Utils.makeNicePrice(profitPrice)

                        // выставить ордер на продажу
                        while (true) {
                            try {
                                position = depositManager.getPositionForFigi(stock.instrument.figi)
                                var lotsLeft = 0
                                if (position != null) lotsLeft = position.lots - position.blocked.toInt()
                                if (lotsLeft == 0) break

                                status = OrderStatus.ORDER_SELL_PREPARE
                                sellLimitOrder = ordersService.placeLimitOrder(
                                    lotsLeft,
                                    stock.instrument.figi,
                                    profitPrice,
                                    OperationType.SELL,
                                    depositManager.getActiveBrokerAccountId()
                                )
                                status = OrderStatus.ORDER_SELL
                                Utils.showToastAlert("$ticker: заявка на продажу по $profitPrice")
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(DelayMiddle)
                        }
                    }
                    delay(DelayMiddle)
                }

                while (true) {
                    delay(DelayLong)
                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position == null) { // продано!
                        status = OrderStatus.SOLD
                        Utils.showToastAlert("$ticker: продано!?")
                        break
                    }
                }

            } catch (e: Exception) {
                status = OrderStatus.CANCELED
                e.printStackTrace()
            }
        }
    }

    fun buyLimitFromAsk(profit: Double) {
        if (lots == 0 || profit == 0.0) return

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val ticker = stock.instrument.ticker

                // получить лучший аск из стакана
                val orderbook = marketService.orderbook(stock.instrument.figi, 10)
                val buyPrice = orderbook.getBestPriceFromAsk(lots)
                log("$orderbook")

                while (true) {
                    try {
                        status = OrderStatus.ORDER_BUY_PREPARE
                        buyLimitOrder = ordersService.placeLimitOrder(
                            lots,
                            stock.instrument.figi,
                            buyPrice,
                            OperationType.BUY,
                            depositManager.getActiveBrokerAccountId()
                        )
                        status = OrderStatus.ORDER_BUY
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                Utils.showToastAlert("$ticker: покупка по $buyPrice")

                // проверяем появился ли в портфеле тикер
                var position: PortfolioPosition?
                while (true) {
                    try {
                        depositManager.refreshDeposit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position != null && position.lots >= lots) { // куплено!
                        status = OrderStatus.BOUGHT
                        Utils.showToastAlert("$ticker: куплено!")
                        break
                    }

                    delay(DelayLong)
                }

                // продаём
                position?.let {
                    if (profit == 0.0 || buyPrice == 0.0) return@launch
                    var profitPrice = buyPrice + buyPrice / 100.0 * profit
                    profitPrice = Utils.makeNicePrice(profitPrice)

                    // выставить ордер на продажу
                    while (true) {
                        try {
                            status = OrderStatus.ORDER_SELL_PREPARE
                            sellLimitOrder = ordersService.placeLimitOrder(
                                it.lots,
                                stock.instrument.figi,
                                profitPrice,
                                OperationType.SELL,
                                depositManager.getActiveBrokerAccountId()
                            )
                            status = OrderStatus.ORDER_SELL
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(DelayFast)
                    }

                    Utils.showToastAlert("$ticker: заявка на продажу по $profitPrice")
                }

                while (true) {
                    delay(DelayLong)

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position == null) { // продано!
                        status = OrderStatus.SOLD
                        Utils.showToastAlert("$ticker: продано!")
                        break
                    }
                }

            } catch (e: Exception) {
                status = OrderStatus.CANCELED
                e.printStackTrace()
            }
        }
    }

    fun buyFromAsk2358() {
        if (lots == 0) return

        GlobalScope.launch(Dispatchers.Main) {
            try {
                var buyPrice: Double
                while (true) {
                    try {
                        status = OrderStatus.ORDER_BUY_PREPARE
                        // получить стакан
                        val orderbook = marketService.orderbook(stock.instrument.figi, 5)
                        log("$orderbook")

                        buyPrice = orderbook.getBestPriceFromAsk(lots)
                        if (buyPrice == 0.0) return@launch

                        buyLimitOrder = ordersService.placeLimitOrder(
                            lots,
                            stock.instrument.figi,
                            buyPrice,
                            OperationType.BUY,
                            depositManager.getActiveBrokerAccountId()
                        )
                        status = OrderStatus.ORDER_BUY
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                val ticker = stock.instrument.ticker
                Utils.showToastAlert("$ticker: покупка по $buyPrice")

                delay(DelayFast)

                // проверяем появился ли в портфеле тикер
                var position: PortfolioPosition?
                while (true) {
                    try {
                        depositManager.refreshDeposit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position != null && position.lots >= lots) {
                        status = OrderStatus.BOUGHT
                        Utils.showToastAlert("$ticker: куплено!")
                        break
                    }

                    delay(DelayLong)
                }

                if (trailingTake) { // запускаем трейлинг стоп
                    currentTrailingTakeProfit = TrailingTakeProfit(stock, buyPrice, trailingTakeActivationPercent, trailingTakeStopDelta)
                    status = OrderStatus.ORDER_SELL_TRAILING
                    var profitSellPrice = currentTrailingTakeProfit?.process() ?: 0.0
                    status = OrderStatus.ORDER_SELL_PREPARE
                    if (profitSellPrice == 0.0) return@launch

                    // выставить ордер на продажу
                    while (true) {
                        try {
                            profitSellPrice = Utils.makeNicePrice(profitSellPrice)
                            sellLimitOrder = ordersService.placeLimitOrder(
                                lots,
                                stock.instrument.figi,
                                profitSellPrice,
                                OperationType.SELL,
                                depositManager.getActiveBrokerAccountId()
                            )
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(DelayFast)
                    }
                    status = OrderStatus.ORDER_SELL
                } else { // продажа лесенкой
                    // продаём 2358 лесенкой
                    position?.let {
                        val totalLots = it.lots
                        var profitFrom = percentProfitSellFrom
                        var profitTo = percentProfitSellTo

                        if (profitFrom == 0.0) {
                            profitFrom = SettingsManager.get2358TakeProfitFrom()
                        }

                        if (profitTo == 0.0) {
                            profitTo = SettingsManager.get2358TakeProfitTo()
                        }

                        val profitStep = SettingsManager.get2358TakeProfitStep()

                        // в случае кривых настроек просто не создаём заявки
                        if (profitTo < profitFrom || profitStep == 0 || profitFrom == 0.0 || profitTo == 0.0) return@launch

                        val list: MutableList<Pair<Int, Double>> = mutableListOf()
                        when (profitStep) {
                            1 -> { // если шаг 1, то создать заявку на нижний % и всё
                                list.add(Pair(totalLots, profitFrom))
                            }
                            2 -> { // первый и последний
                                val partLots1 = totalLots / 2
                                val partLots2 = totalLots - partLots1
                                list.add(Pair(partLots1, profitFrom))
                                list.add(Pair(partLots2, profitTo))
                            }
                            else -> { // промежуточные
                                val profitStepDouble: Double = profitStep.toDouble()
                                val delta = (profitTo - profitFrom) / (profitStep - 1)

                                // округляем в бОльшую, чтобы напоследок осталось мало лотов
                                val basePartLots: Int = ceil(totalLots / profitStepDouble).toInt()

                                var currentLots = basePartLots
                                var currentProfit = profitFrom

                                // стартовый профит
                                list.add(Pair(basePartLots, currentProfit))

                                var step = profitStep - 2
                                while (step > 0) {
                                    if (currentLots + basePartLots > totalLots) {
                                        break
                                    }
                                    currentLots += basePartLots
                                    currentProfit += delta
                                    list.add(Pair(basePartLots, currentProfit))
                                    step--
                                }

                                // финальный профит
                                val lastPartLots = totalLots - currentLots
                                if (lastPartLots > 0) {
                                    list.add(Pair(lastPartLots, profitTo))
                                }
                            }
                        }

                        if (list.isEmpty()) return@launch

                        status = OrderStatus.ORDER_SELL_PREPARE
                        for (p in list) {
                            val lots = p.first
                            val profit = p.second

                            // вычисляем и округляем до 2 после запятой
                            var profitPrice = buyPrice + buyPrice / 100.0 * profit
                            profitPrice = Utils.makeNicePrice(profitPrice)

                            if (lots <= 0 || profitPrice == 0.0) continue

                            // выставить ордер на продажу
                            while (true) {
                                try {
                                    sellLimitOrder = ordersService.placeLimitOrder(
                                        lots,
                                        stock.instrument.figi,
                                        profitPrice,
                                        OperationType.SELL,
                                        depositManager.getActiveBrokerAccountId()
                                    )
                                    break
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                delay(DelayFast)
                            }
                        }
                        status = OrderStatus.ORDER_SELL
                        Utils.showToastAlert("$ticker: заявка на продажу по $profitFrom")
                    }
                }

                while (true) {
                    delay(DelayLong)

                    position = depositManager.getPositionForFigi(stock.instrument.figi)
                    if (position == null) { // продано!
                        status = OrderStatus.SOLD
                        Utils.showToastAlert("$ticker: продано!")
                        break
                    }
                }
            } catch (e: Exception) {
                status = OrderStatus.CANCELED
                e.printStackTrace()
            }
        }
    }

    fun sell() {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val figi = stock.instrument.figi
                val pos = depositManager.getPositionForFigi(figi)
                if (pos == null) {
                    status = OrderStatus.WTF_2
                    return@launch
                }

                position = pos
                if (pos.lots == 0 || percentProfitSellFrom == 0.0) {
                    status = OrderStatus.WTF_3
                    return@launch
                }

                // скорректировать процент профита в зависимости от свечи открытия
                val startPrice = pos.stock?.closePrices?.close_post ?: 0.0
                val currentPrice = pos.stock?.candleToday?.closingPrice ?: 0.0
                if (startPrice != 0.0 && currentPrice != 0.0) {
                    val change = (100.0 * currentPrice) / startPrice - 100.0
                    if (change > percentProfitSellFrom) { // если изменение больше текущего профита, то приблизить его к этой цене
                        var delta = abs(change) - abs(percentProfitSellFrom)

                        // 0.50 коэф приближения к нижней точке, в самом низу могут не налить
                        delta *= 0.50

                        // корректируем % профита продажи
                        percentProfitSellFrom = abs(percentProfitSellFrom) + delta
                    }
                }

                val profitPrice = getProfitPriceForSell()
                if (profitPrice == 0.0) {
                    status = OrderStatus.WTF_4
                    return@launch
                }

                while (true) {
                    try {
                        // выставить ордер на продажу
                        status = OrderStatus.ORDER_SELL_PREPARE
                        sellLimitOrder = ordersService.placeLimitOrder(
                            pos.lots,
                            figi,
                            profitPrice,
                            OperationType.SELL,
                            depositManager.getActiveBrokerAccountId()
                        )
                        delay(DelayMiddle)
                        depositManager.refreshOrders()
                        if (depositManager.getOrderAllOrdersForFigi(figi, OperationType.SELL).isEmpty()) {
                            continue
                        }
                        status = OrderStatus.ORDER_SELL
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(DelayFast)
                }

                // проверяем продалось или нет
                while (true) {
                    delay(DelayLong)

                    val p = depositManager.getPositionForFigi(figi)
                    if (p == null) { // продано!
                        status = OrderStatus.SOLD
                        break
                    }
                }
            } catch (e: Exception) {
                if (status != OrderStatus.ORDER_SELL) {
                    status = OrderStatus.CANCELED
                }
                e.printStackTrace()
            }
        }
    }

    fun getProfitPriceForSell(): Double {
        if (percentProfitSellFrom == 0.0) return 0.0

        val avg = position.getAveragePrice()
        val priceProfit = avg + avg / 100.0 * percentProfitSellFrom
        return Utils.makeNicePrice(priceProfit)
    }

    fun processInitialProfit() {
        // по умолчанию взять профит из настроек
        var futureProfit = SettingsManager.get1000SellTakeProfit()

        // если не задан в настройках, то 1% по умолчанию
        if (futureProfit == 0.0) futureProfit = 1.0

        // если текущий профит уже больше, то за базовый взять его
        val change = position.getProfitAmount()
        val totalCash = position.balance * position.getAveragePrice()
        val currentProfit = (100.0 * change) / totalCash

        percentProfitSellFrom = if (currentProfit > futureProfit) currentProfit else futureProfit
        status = OrderStatus.WAITING
    }
}
