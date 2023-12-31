package com.project.ti2358.data.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.project.ti2358.R
import com.project.ti2358.TheApplication
import com.project.ti2358.data.tinkoff.model.Currency
import com.project.ti2358.data.daager.model.PresetStock
import com.project.ti2358.service.Utils
import org.koin.core.component.KoinApiExtension
import java.lang.Integer.parseInt
import java.util.*

@KoinApiExtension
class SettingsManager {

    companion object {
        lateinit var preferences: SharedPreferences

        fun setSettingsContext(applicationContext: Context) {
            preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        }

        fun isAllowCurrency(currency: Currency?): Boolean {
            if (currency == Currency.USD) return true
            return false
        }

        fun getDarkTheme(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_dark_theme)
            return preferences.getBoolean(key, false)
        }

        /******************** broker *************************/

        fun getBrokerTinkoff(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_broker_tinkoff)
            return preferences.getBoolean(key, true)
        }

        fun getBrokerAlor(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_broker_alor)
            return preferences.getBoolean(key, false)
        }

        /******************** tinkoff *************************/

        fun getTokenTinkoff(): String {
            val tokenKey = TheApplication.application.applicationContext.getString(R.string.setting_key_token_market)
            return preferences.getString(tokenKey, "")?.trim() ?: ""
        }

        fun getActiveBaseUrlTinkoff(): String = "https://api-invest.tinkoff.ru/openapi/"

        /******************** alor *************************/

        fun getAlorQuotes(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_alor_quotes)
            return preferences.getBoolean(key, false)
        }

        fun getAlorOrdebook(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_alor_orderbook)
            return preferences.getBoolean(key, false)
        }

        fun getAlorToken(): String {
            val tokenKey = TheApplication.application.applicationContext.getString(R.string.setting_key_token_market_alor)
            return preferences.getString(tokenKey, "")?.trim() ?: ""
        }

        fun getAlorUsername(): String {
            val value = TheApplication.application.applicationContext.getString(R.string.setting_key_alor_username)
            return preferences.getString(value, "")?.trim() ?: ""
        }

        fun getAlorMultiplierMoney(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_alor_multiplier_money)
            val value: String? = preferences.getString(key, "0.5")
            return try {
                (value ?: "0.5").toDouble()
            } catch (e: Exception) {
                0.5
            }
        }

        /******************** pantini *************************/

        fun getPantiniTelegramID(): String {
            val tokenKey = TheApplication.application.applicationContext.getString(R.string.setting_key_pantini_telegram_id)
            return preferences.getString(tokenKey, "")?.trim() ?: ""
        }

        fun getPantiniWardenToken(): String {
            val tokenKey = TheApplication.application.applicationContext.getString(R.string.setting_key_pantini_warden_token)
            return preferences.getString(tokenKey, "")?.trim() ?: ""
        }

        /******************** common *************************/

        fun getCommonPriceMin(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_stocks_usd_price_min)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun getCommonPriceMax(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_stocks_usd_price_max)
            val value: String? = preferences.getString(key, "5000")
            return try {
                parseInt(value ?: "5000")
            } catch (e: Exception) {
                5000
            }
        }

        fun isAllowRus(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_stocks_allow_rus)
            return preferences.getBoolean(key, false)
        }

        /******************** Trailing take *************************/

        fun getTrailingStopTakeProfitPercentActivation(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trailing_stop_take_profit_percent_activation)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun getTrailingStopTakeProfitPercentDelta(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trailing_stop_take_profit_percent_delta)
            val value: String? = preferences.getString(key, "0.25")
            return try {
                (value ?: "0.25").toDouble()
            } catch (e: Exception) {
                0.25
            }
        }

        fun getTrailingStopStopLossPercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trailing_stop_stop_loss_percent)
            val value: String? = preferences.getString(key, "0.0")
            return try {
                (value ?: "0.0").toDouble()
            } catch (e: Exception) {
                0.0
            }
        }

        fun getTrailingStopSellBestBid(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trailing_stop_sell_best_bid)
            return preferences.getBoolean(key, false)
        }

        /******************** orderbook *************************/

        fun getOrderbookVolumes(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_orderbook_volumes)
            val value: String? = preferences.getString(key, "1 5 10 50 100 500 1000 5000")
            return value ?: "1 5 10 50 100 500 1000 5000"
        }

        fun getOrderbookPrices(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_orderbook_prices)
            val value: String? = preferences.getString(key, "1 2 5 10")
            return value ?: "1 2 5 10"
        }

        /******************** 2358 *************************/

        fun get2358ChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_price_change_percent)
            val value: String? = preferences.getString(key, "-1")
            return try {
                (value ?: "-1").toDouble()
            } catch (e: Exception) {
                -1.0
            }
        }

        fun get2358PurchaseTime(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_purchase_time)
            val value: String? = preferences.getString(key, "22:58:00")
            return value ?: "22:58:00"
        }

        fun get2358PurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_purchase_volume)
            val value: String? = preferences.getString(key, "1000")
            return try {
                parseInt(value ?: "1000")
            } catch (e: Exception) {
                1000
            }
        }

        fun get2358TakeProfitFrom(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_take_profit_from)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get2358TakeProfitTo(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_take_profit_to)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get2358TakeProfitStep(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_take_profit_step)
            val value: String? = preferences.getString(key, "1")
            return try {
                parseInt(value ?: "1")
            } catch (e: Exception) {
                1
            }
        }

        fun get2358VolumeDayPieces(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_volume_min_day)
            val value: String? = preferences.getString(key, "10000")
            return try {
                parseInt(value ?: "10000")
            } catch (e: Exception) {
                10000
            }
        }

        fun get2358VolumeDayCash(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2358_volume_min_cash)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        /******************** Love *************************/

        fun getLoveSet(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_love_set)
            val value: String? = preferences.getString(key, "SPCE TAL")?.trim()
            val array = value?.toUpperCase()?.split(" ")
            return array ?: emptyList()
        }

        /******************** Black *************************/

        fun getBlackSet(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_black_set)
            val value: String? = preferences.getString(key, "LPL ACH")?.trim()
            val array = value?.toUpperCase()?.split(" ")
            return array ?: emptyList()
        }

        /******************** Premarket *************************/

        fun getPremarketChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_premarket_price_change_percent)
            val value: String? = preferences.getString(key, "0")
            return try {
                (value ?: "0").toDouble()
            } catch (e: Exception) {
                0.0
            }
        }

        fun getPremarketVolumeMin(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_premarket_volume_min_day)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun getPremarketVolumeMax(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_premarket_volume_max_day)
            val value: String? = preferences.getString(key, "10000000")
            return try {
                parseInt(value ?: "10000000")
            } catch (e: Exception) {
                10000000
            }
        }

        fun getPremarketOnlyLove(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_premarket_only_love)
            return preferences.getBoolean(key, false)
        }

        fun getPremarketOnlyMorning(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_premarket_only_morning)
            return preferences.getBoolean(key, false)
        }
        /******************** 1000 sell *************************/

        private fun stringToPresetStocks(value: String): List<PresetStock> {
            val array = value.toUpperCase().split("\n")
            val presetStocks: MutableList<PresetStock> = mutableListOf()
            array.forEach {
                try {
                    val params = it.split(" ")
                    if (params.size == 4) {
                        val preset = PresetStock(params[0], params[1].toDouble(), params[2].toInt(), params[3].toDouble())
                        presetStocks.add(preset)
                    } else if (params.size == 3) {
                        val preset = PresetStock(params[0], params[1].toDouble(), params[2].toInt(), 0.0)
                        presetStocks.add(preset)
                    } else if (params.size == 2) {
                        val preset = PresetStock(params[0], params[1].toDouble(), 0, 0.0)
                        presetStocks.add(preset)
                    } else if (params.size == 1) {
                        val preset = PresetStock(params[0], 0.0, 0, 0.0)
                        presetStocks.add(preset)
                    }
                } catch (e: java.lang.Exception) {

                }
            }
            return presetStocks
        }

        fun get1000SellTakeProfitDepo(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_take_profit_depo)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1000SellTakeProfitShort(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_take_profit_short)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1000SellTakeProfitBuy(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_take_profit_buy)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1000SellSet(numberSet: Int): List<PresetStock> {
            val key = when (numberSet) {
                1 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_set_1)
                2 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_set_2)
                3 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_set_3)
                4 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_set_4)
                else -> return emptyList()
            }
            val value: String = preferences.getString(key, "SPCE 1.0 10 0.5\nTAL 1.0 10 0.5")?.trim() ?: ""
            return stringToPresetStocks(value)
        }

        fun get1000SellOrderLifeTimeSecondsDepo(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_order_lifetime_seconds_depo)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun get1000SellOrderLifeTimeSecondsShort(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_order_lifetime_seconds_short)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        fun get1000SellOrderLifeTimeSecondsBuy(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_sell_order_lifetime_seconds_buy)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }
        /******************** 1000 buy *************************/

        fun get1000BuySet(numberSet: Int): List<PresetStock> {
            val key = when (numberSet) {
                1 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_set_1)
                2 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_set_2)
                3 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_set_3)
                4 -> TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_set_4)
                else -> return emptyList()
            }
            val value: String = preferences.getString(key, "SPCE -1.0 10 0.5\nTAL -1.0 10 0.5")?.trim() ?: ""
            return stringToPresetStocks(value)
        }

        fun get1000BuyPurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_purchase_volume)
            val value: String? = preferences.getString(key, "500")
            return try {
                parseInt(value ?: "500")
            } catch (e: Exception) {
                500
            }
        }

        fun get1000BuyTakeProfitDepo(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_take_profit_depo)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1000BuyTakeProfitLong(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_take_profit_long)
            val value: String? = preferences.getString(key, "-1.0")
            return try {
                (value ?: "-1.0").toDouble()
            } catch (e: Exception) {
                -1.0
            }
        }

        fun get1000BuyTakeProfitSell(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_take_profit_sell)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1000BuyOrderLifeTimeSecondsDepo(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_order_lifetime_seconds_depo)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun get1000BuyOrderLifeTimeSecondsLong(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_order_lifetime_seconds_long)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        fun get1000BuyOrderLifeTimeSecondsSell(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1000_buy_order_lifetime_seconds_sell)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        /******************** 1728 *************************/

        fun get1728ChangePercent(step: Int): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_price_change_steps)
            val value: String? = preferences.getString(key, "1.0 1.0 1.0")
            val volumes = value?.split(" ") ?: return 1.0
            if (volumes.size < 3) return 1.0

            return try {
                volumes[step].toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1728PurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_purchase_volume)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun get1728TakeProfit(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_take_profit)
            val value: String? = preferences.getString(key, "1")
            return try {
                (value ?: "1").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get1728TrailingStop(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_trailing_stop)
            return preferences.getBoolean(key, false)
        }

        fun get1728CalcFromOS(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_calc_from_os)
            return preferences.getBoolean(key, false)
        }

        fun get1728Volume(step: Int): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_1728_volume_min_each_steps)
            val value: String? = preferences.getString(key, "1000 2000 3000")
            val volumes = value?.split(" ") ?: return 2000
            if (volumes.size < 3) return 2000

            return try {
                parseInt(volumes[step])
            } catch (e: Exception) {
                2000
            }
        }

        /******************** THE TAZIK *************************/

        fun getTazikSet1(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_set_1)
            val value: String? = preferences.getString(key, "")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikSet2(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_set_2)
            val value: String? = preferences.getString(key, "")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikSet3(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_set_3)
            val value: String? = preferences.getString(key, "")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_min_percent_to_buy)
            val value: String? = preferences.getString(key, "-1.0")
            return try {
                (value ?: "-1.0").toDouble()
            } catch (e: Exception) {
                -1.0
            }
        }

        fun getTazikMinVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_min_volume)
            val value: String? = preferences.getString(key, "100")
            return try {
                parseInt(value ?: "100")
            } catch (e: Exception) {
                100
            }
        }

        fun getTazikOrderLifeTimeSeconds(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_order_lifetime_seconds)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        fun getTazikPurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_purchase_volume)
            val value: String? = preferences.getString(key, "500")
            return try {
                parseInt(value ?: "500")
            } catch (e: Exception) {
                500
            }
        }

        fun getTazikPurchaseParts(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_purchase_parts)
            val value: String? = preferences.getString(key, "2")
            return try {
                (value ?: "2").toInt()
            } catch (e: Exception) {
                2
            }
        }

        fun getTazikTakeProfit(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_take_profit)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun getTazikApproximationFactor(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_approximation_factor)
            val value: String? = preferences.getString(key, "0.65")
            return try {
                (value ?: "0.65").toDouble()
            } catch (e: Exception) {
                0.65
            }
        }

        fun getTazikAllowAveraging(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_allow_averaging)
            return preferences.getBoolean(key, false)
        }

        fun getTazikNearestTime(): Pair<String, String> {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_schedule_from_to)
            val time = preferences.getString(key, "06:59:50-07:15:00 09:59:50-10:15:00 10:59:50-11:15:00 15:29:50-15:45:00")

            if (time != null && time != "") {
                return processNearestTimeFromTo(time)
            }

            return Pair("", "")
        }

        fun getTazikExcludeReports(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_exclude_report)
            return preferences.getBoolean(key, true)
        }

        fun getTazikExcludeDivs(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_exclude_divs)
            return preferences.getBoolean(key, true)
        }

        fun getTazikExcludeFDA(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_exclude_fda)
            return preferences.getBoolean(key, true)
        }

        fun getTazikExcludeDepo(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_exclude_depo)
            return preferences.getBoolean(key, true)
        }

        fun getTazikVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_voice)
            return preferences.getBoolean(key, true)
        }
        /******************** THE TAZIK ENDLESS *************************/

        fun getTazikEndlessSet1(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set)
            val value: String? = preferences.getString(key, "SPCE GTHX ARCT BLUE")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikEndlessSet2(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set_2)
            val value: String? = preferences.getString(key, "VIPS ACH BABA JD BIDU")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikEndlessSet3(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_set_3)
            val value: String? = preferences.getString(key, "VTBR OZON MGNT BELU")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getTazikEndlessResetIntervalSeconds(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_reset_interval_seconds)
            val value: String? = preferences.getString(key, "300")
            return try {
                parseInt(value ?: "300")
            } catch (e: Exception) {
                300
            }
        }

        fun getTazikEndlessMinVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_min_volume)
            val value: String? = preferences.getString(key, "100")
            return try {
                parseInt(value ?: "100")
            } catch (e: Exception) {
                100
            }
        }

        fun getTazikEndlessDayMinVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_day_min_volume)
            val value: String? = preferences.getString(key, "100")
            return try {
                parseInt(value ?: "100")
            } catch (e: Exception) {
                100
            }
        }

        fun getTazikEndlessOrderLifeTimeSeconds(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_order_lifetime_seconds)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        fun getTazikEndlessChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_min_percent_to_buy)
            val value: String? = preferences.getString(key, "-1.0")
            return try {
                (value ?: "-1.0").toDouble()
            } catch (e: Exception) {
                -1.0
            }
        }

        fun getTazikEndlessPurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_purchase_volume)
            val value: String? = preferences.getString(key, "500")
            return try {
                parseInt(value ?: "500")
            } catch (e: Exception) {
                500
            }
        }

        fun getTazikEndlessPurchaseParts(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_purchase_parts)
            val value: String? = preferences.getString(key, "2")
            return try {
                (value ?: "2").toInt()
            } catch (e: Exception) {
                2
            }
        }

        fun getTazikEndlessTakeProfit(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_take_profit)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun getTazikEndlessApproximationFactor(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_approximation_factor)
            val value: String? = preferences.getString(key, "0.65")
            return try {
                (value ?: "0.65").toDouble()
            } catch (e: Exception) {
                0.65
            }
        }

        fun getTazikEndlessAllowAveraging(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_allow_averaging)
            return preferences.getBoolean(key, false)
        }

        fun getTazikEndlessExcludeReports(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_exclude_report)
            return preferences.getBoolean(key, true)
        }

        fun getTazikEndlessExcludeDivs(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_exclude_divs)
            return preferences.getBoolean(key, true)
        }

        fun getTazikEndlessExcludeFDA(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_exclude_fda)
            return preferences.getBoolean(key, true)
        }

        fun getTazikEndlessExcludeDepo(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_exclude_depo)
            return preferences.getBoolean(key, true)
        }

        fun getTazikEndlessSpikeProtection(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_spike_protection)
            val value: String? = preferences.getString(key, "1")
            return try {
                parseInt(value ?: "1")
            } catch (e: Exception) {
                1
            }
        }

        fun getTazikEndlessClosePriceProtectionPercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_close_price_protection_percent)
            val value: String? = preferences.getString(key, "-0.1")
            return try {
                (value ?: "-0.1").toDouble()
            } catch (e: Exception) {
                -0.1
            }
        }

        fun processNearestTimeFromTo(time: String): Pair<String, String> {
            val timesFromTo = time.split(" ").toTypedArray()

            // отсортировать по возрастанию времени старта
            timesFromTo.sortBy { t ->
                val fromTo = t.split("-").toTypedArray()
                val dayTimeFrom = fromTo[0].split(":").toTypedArray()
                parseInt(dayTimeFrom[0]) * 3600 + parseInt(dayTimeFrom[1]) * 60 + parseInt(dayTimeFrom[2])
            }

            var finalTimeFrom = ""
            var finalTimeTo = ""

            for (t in timesFromTo) {
                var timeFrom = ""
                var timeTo = ""
                val fromTo = t.split("-").toTypedArray()
                if (fromTo.size > 0) { // from
                    timeFrom = fromTo[0]
                    val dayTime = timeFrom.split(":").toTypedArray()
                    if (dayTime.size < 3) {
                        finalTimeFrom = ""
                        continue
                    }

                    val hours: Int
                    val minutes: Int
                    val seconds: Int
                    try {
                        hours = parseInt(dayTime[0])
                        minutes = parseInt(dayTime[1])
                        seconds = parseInt(dayTime[2])
                    } catch (e: Exception) {
                        Utils.showToastAlert("Неверный формат времени в настройках!")
                        continue
                    }
                    val currentMskTime = Utils.getTimeMSK()

                    val hoursMsk = currentMskTime.get(Calendar.HOUR_OF_DAY)
                    val minutesMsk = currentMskTime.get(Calendar.MINUTE)
                    val secondsMsk = currentMskTime.get(Calendar.SECOND)

                    val total = hours * 3600 + minutes * 60 + seconds
                    val totalMsk = hoursMsk * 3600 + minutesMsk * 60 + secondsMsk
                    if (totalMsk < total) {
                        finalTimeFrom = fromTo[0]
                    } else {
                        continue
                    }
                }

                if (fromTo.size > 1) { // to
                    timeTo = fromTo[1]
                    val dayTime = timeTo.split(":").toTypedArray()
                    if (dayTime.size < 3) {
                        finalTimeTo = ""
                    }

                    val hours: Int
                    val minutes: Int
                    val seconds: Int
                    try {
                        hours = parseInt(dayTime[0])
                        minutes = parseInt(dayTime[1])
                        seconds = parseInt(dayTime[2])

                        val currentMskTime = Utils.getTimeMSK()

                        val hoursMsk = currentMskTime.get(Calendar.HOUR_OF_DAY)
                        val minutesMsk = currentMskTime.get(Calendar.MINUTE)
                        val secondsMsk = currentMskTime.get(Calendar.SECOND)

                        val total = hours * 3600 + minutes * 60 + seconds
                        val totalMsk = hoursMsk * 3600 + minutesMsk * 60 + secondsMsk
                        if (totalMsk < total) {
                            finalTimeTo = fromTo[1]
                        }
                    } catch (e: Exception) {
                        Utils.showToastAlert("Неверный формат времени в настройках!")
                        finalTimeTo = ""
                    }
                }

                if (timeFrom != "") break
            }

            return Pair(finalTimeFrom, finalTimeTo)
        }

        fun getTazikEndlessNearestTime(): Pair<String, String> {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_schedule_from_to)
            val time = preferences.getString(key, "06:59:50-07:15:00 09:59:50-10:15:00 10:59:50-11:15:00 15:29:50-15:45:00")

            if (time != null && time != "") {
                return processNearestTimeFromTo(time)
            }

            return Pair("", "")
        }

        fun getTazikEndlessVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_tazik_endless_voice)
            return preferences.getBoolean(key, true)
        }

        /******************** THE ZONTIK ENDLESS *************************/

        fun getZontikEndlessSet1(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_set)
            val value: String? = preferences.getString(key, "SPCE GTHX")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getZontikEndlessSet2(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_set_2)
            val value: String? = preferences.getString(key, "VIPS BABA JD BIDU")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getZontikEndlessSet3(): List<String> {
            val key = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_set_3)
            val value: String? = preferences.getString(key, "VTBR OZON MGNT BELU")?.trim()
            val array = value?.split(" ")
            return array ?: emptyList()
        }

        fun getZontikEndlessResetIntervalSeconds(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_reset_interval_seconds)
            val value: String? = preferences.getString(key, "60")
            return try {
                parseInt(value ?: "60")
            } catch (e: Exception) {
                60
            }
        }

        fun getZontikEndlessMinVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_min_volume)
            val value: String? = preferences.getString(key, "100")
            return try {
                parseInt(value ?: "100")
            } catch (e: Exception) {
                100
            }
        }

        fun getZontikEndlessDayMinVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_day_min_volume)
            val value: String? = preferences.getString(key, "100")
            return try {
                parseInt(value ?: "100")
            } catch (e: Exception) {
                100
            }
        }

        fun getZontikEndlessOrderLifeTimeSeconds(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_order_lifetime_seconds)
            val value: String? = preferences.getString(key, "120")
            return try {
                parseInt(value ?: "120")
            } catch (e: Exception) {
                120
            }
        }

        fun getZontikEndlessChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_min_percent_to_buy)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun getZontikEndlessPurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_purchase_volume)
            val value: String? = preferences.getString(key, "500")
            return try {
                parseInt(value ?: "500")
            } catch (e: Exception) {
                500
            }
        }

        fun getZontikEndlessPurchaseParts(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_purchase_parts)
            val value: String? = preferences.getString(key, "2")
            return try {
                (value ?: "2").toInt()
            } catch (e: Exception) {
                2
            }
        }

        fun getZontikEndlessTakeProfit(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_take_profit)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun getZontikEndlessApproximationFactor(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_approximation_factor)
            val value: String? = preferences.getString(key, "0.65")
            return try {
                (value ?: "0.65").toDouble()
            } catch (e: Exception) {
                0.65
            }
        }

        fun getZontikEndlessAllowAveraging(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_allow_averaging)
            return preferences.getBoolean(key, false)
        }

        fun getZontikEndlessExcludeReports(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_exclude_report)
            return preferences.getBoolean(key, true)
        }

        fun getZontikEndlessExcludeDivs(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_exclude_divs)
            return preferences.getBoolean(key, true)
        }

        fun getZontikEndlessExcludeFDA(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_exclude_fda)
            return preferences.getBoolean(key, true)
        }

        fun getZontikEndlessExcludeDepo(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_exclude_depo)
            return preferences.getBoolean(key, true)
        }

        fun getZontikEndlessSpikeProtection(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_spike_protection)
            val value: String? = preferences.getString(key, "1")
            return try {
                parseInt(value ?: "1")
            } catch (e: Exception) {
                1
            }
        }

        fun getZontikEndlessClosePriceProtectionPercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_close_price_protection_percent)
            val value: String? = preferences.getString(key, "0.1")
            return try {
                (value ?: "0.1").toDouble()
            } catch (e: Exception) {
                0.1
            }
        }

        fun getZontikEndlessNearestTime(): Pair<String, String> {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_schedule_from_to)
            val time = preferences.getString(key, "06:59:50-07:15:00 09:59:50-10:15:00 10:59:50-11:15:00 15:29:50-15:45:00")

            if (time != null && time != "") {
                return processNearestTimeFromTo(time)
            }

            return Pair("", "")
        }

        fun getZontikEndlessVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_zontik_endless_voice)
            return preferences.getBoolean(key, true)
        }

        /******************** Rockets *************************/
        fun getRocketChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_change_percent)
            val value: String? = preferences.getString(key, "3.0")
            return try {
                (value ?: "3.0").toDouble()
            } catch (e: Exception) {
                3.0
            }
        }

        fun getRocketChangeMinutes(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_change_minutes)
            val value: String? = preferences.getString(key, "10")
            return try {
                (value ?: "10").toInt()
            } catch (e: Exception) {
                10
            }
        }

        fun getRocketChangeVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_change_volume)
            val value: String? = preferences.getString(key, "0")
            return try {
                (value ?: "0").toInt()
            } catch (e: Exception) {
                0
            }
        }

        fun getRocketNotifyAlive(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_notify_alive_seconds)
            val value: String? = preferences.getString(key, "60")
            return try {
                (value ?: "60").toInt()
            } catch (e: Exception) {
                60
            }
        }

        fun getRocketVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_voice)
            return preferences.getBoolean(key, true)
        }

        fun getRocketOnlyLove(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_rocket_only_love)
            return preferences.getBoolean(key, false)
        }

        /******************** Telegram *************************/
        fun getTelegramAutostart(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_autostart)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramBotApiKey(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_bot_api_key)
            val value: String? = preferences.getString(key, "")
            return value ?: ""
        }

        fun getTelegramChatID(): List<Long> {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_channel_id)
            val value: String? = preferences.getString(key, "")
            val ids = value?.split(" ") ?: return emptyList()
            return ids.map { it.toLong() }
        }


        fun getTelegramUpdateDelay(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_update_delay)
            val value: String? = preferences.getString(key, "10")
            return try {
                parseInt(value ?: "10")
            } catch (e: Exception) {
                10
            }
        }

        fun getTelegramHello(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_hello)
            val value: String? = preferences.getString(key, "Бот запущен 🦌")
            return value ?: "Бот запущен 🦌"
        }

        fun getTelegramBye(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_bye)
            val value: String? = preferences.getString(key, "Бот остановлен! 🐹")
            return value ?: "Бот остановлен! 🐹"
        }

        fun getTelegramSendTrades(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_trades)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramSendOrders(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_orders)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramSendRockets(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_rockets)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramSendTaziks(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_taziks)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramSendSpikes(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_spikes)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramSendTrends(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_trends)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramSendLimits(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_limits)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramSendGotoTerminal(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_send_goto_terminal)
            return preferences.getBoolean(key, true)
        }

        fun getTelegramAllowCommandInform(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_allow_command_inform)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramAllowCommandHandle(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_allow_command_handle)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramAllowCommandBuySell(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_allow_command_buy_sell)
            return preferences.getBoolean(key, false)
        }

        fun getTelegramAllowShowDepo(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_telegram_allow_command_depo)
            return preferences.getBoolean(key, false)
        }

        /******************** 2225 short *************************/

        fun get2225ChangePercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_price_change_percent)
            val value: String? = preferences.getString(key, "1")
            return try {
                (value ?: "1").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get2225PurchaseTime(): String {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_purchase_time)
            val value: String? = preferences.getString(key, "22:24:00")
            return value ?: "22:24:00"
        }

        fun get2225PurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_purchase_volume)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun get2225TakeProfitFrom(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_take_profit_from)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get2225TakeProfitTo(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_take_profit_to)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        fun get2225TakeProfitStep(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_take_profit_step)
            val value: String? = preferences.getString(key, "1")
            return try {
                parseInt(value ?: "1")
            } catch (e: Exception) {
                1
            }
        }

        fun get2225VolumeDayPieces(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_volume_min_day)
            val value: String? = preferences.getString(key, "150000")
            return try {
                parseInt(value ?: "150000")
            } catch (e: Exception) {
                150000
            }
        }

        fun get2225VolumeDayCash(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_2225_volume_min_cash)
            val value: String? = preferences.getString(key, "1.0")
            return try {
                (value ?: "1.0").toDouble()
            } catch (e: Exception) {
                1.0
            }
        }

        /******************** Follower *************************/
        fun getFollowerIds(): List<Long> {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_follower_pastuh_id)
            val value: String? = preferences.getString(key, "")
            val ids = value?.split(" ") ?: return emptyList()
            return ids.map { it.toLong() }
        }

        fun getFollowerPurchaseVolume(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_follower_purchase_volume)
            val value: String? = preferences.getString(key, "250")
            return try {
                parseInt(value ?: "250")
            } catch (e: Exception) {
                250
            }
        }

        /******************** Trends *************************/
        fun getTrendMinDownPercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_change_min_down_change_percent)
            val value: String? = preferences.getString(key, "5.0")
            return try {
                (value ?: "5.0").toDouble()
            } catch (e: Exception) {
                5.0
            }
        }

        fun getTrendMinUpPercent(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_change_min_up_change_percent)
            val value: String? = preferences.getString(key, "20.0")
            return try {
                (value ?: "20.0").toDouble()
            } catch (e: Exception) {
                20.0
            }
        }

        fun getTrendAfterMinutes(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_after_minutes)
            val value: String? = preferences.getString(key, "15")
            return try {
                parseInt(value ?: "15")
            } catch (e: Exception) {
                15
            }
        }

        fun getTrendLove(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_only_love)
            return preferences.getBoolean(key, false)
        }

        fun getTrendLong(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_long)
            return preferences.getBoolean(key, true)
        }

        fun getTrendShort(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_short)
            return preferences.getBoolean(key, true)
        }

        fun getTrendVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_trend_voice)
            return preferences.getBoolean(key, true)
        }

        /******************** limits *************************/
        fun getLimitsChangeUp(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_limits_change_up)
            val value: String? = preferences.getString(key, "2.0")
            return try {
                (value ?: "2.0").toDouble()
            } catch (e: Exception) {
                2.0
            }
        }

        fun getLimitsChangeDown(): Double {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_limits_change_down)
            val value: String? = preferences.getString(key, "2.0")
            return try {
                (value ?: "2.0").toDouble()
            } catch (e: Exception) {
                2.0
            }
        }

        fun getLimitsUp(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_limits_up)
            return preferences.getBoolean(key, false)
        }

        fun getLimitsDown(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_limits_down)
            return preferences.getBoolean(key, true)
        }

        fun getLimitsVoice(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_limits_voice)
            return preferences.getBoolean(key, true)
        }

        /******************** Arbitration *************************/
        fun getArbitrationVolumeDayFrom(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_arbitration_volume_day_from)
            val value: String? = preferences.getString(key, "0")
            return try {
                parseInt(value ?: "0")
            } catch (e: Exception) {
                0
            }
        }

        fun getArbitrationVolumeDayTo(): Int {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_arbitration_volume_day_to)
            val value: String? = preferences.getString(key, "1000000")
            return try {
                parseInt(value ?: "1000000")
            } catch (e: Exception) {
                1000000
            }
        }

        fun getArbitrationOnlyLove(): Boolean {
            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_arbitration_only_love)
            return preferences.getBoolean(key, false)
        }

        /******************** fixprice *************************/

        fun getFixPriceTimes(): MutableList<Calendar> {
            val fixList = mutableListOf<Calendar>()

            val key: String = TheApplication.application.applicationContext.getString(R.string.setting_key_fixprice_schedule)
            val time = preferences.getString(key, "07:00:00 10:00:00 11:00:00 16:30:00")

            if (time != null && time != "") {
                val times = time.split(" ").toTypedArray()

                for (t in times) {
                    val dayTime = t.split(":").toTypedArray()
                    if (dayTime.size < 3) continue

                    val hours: Int
                    val minutes: Int
                    val seconds: Int
                    try {
                        hours = parseInt(dayTime[0])
                        minutes = parseInt(dayTime[1])
                        seconds = parseInt(dayTime[2])
                    } catch (e: Exception) {
                        Utils.showToastAlert("Неверный формат времени в настройках!")
                        continue
                    }

                    val fixTime = Utils.getTimeMSK()
                    fixTime.set(Calendar.HOUR_OF_DAY, hours)
                    fixTime.set(Calendar.MINUTE, minutes)
                    fixTime.set(Calendar.SECOND, seconds)
                    fixTime.set(Calendar.MILLISECOND, 0)
                    fixList.add(fixTime)
                }
            }

            return fixList
        }
    }
}
