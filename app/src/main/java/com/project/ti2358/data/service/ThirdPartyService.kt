package com.project.ti2358.data.service

import com.google.gson.JsonObject
import com.project.ti2358.data.api.ThirdPartyApi
import com.project.ti2358.data.manager.SettingsManager
import com.project.ti2358.data.model.dto.daager.*
import com.project.ti2358.service.log
import org.koin.core.component.KoinApiExtension
import retrofit2.Retrofit
import retrofit2.http.Url
import java.lang.Exception

class ThirdPartyService(retrofit: Retrofit) {
    private val thirdPartyApi: ThirdPartyApi = retrofit.create(ThirdPartyApi::class.java)

    @KoinApiExtension
    suspend fun alorRefreshToken(@Url url: String): String {
        val urlToken = url + "?token=${SettingsManager.getActiveTokenAlor()}"
        val json = thirdPartyApi.alorRefreshToken(urlToken)
        return json["AccessToken"].toString().replace("\"", "")
    }

    /* по вопросам предоставления доступа к запросам писать в телегу: @daager 🤙 */
    suspend fun daagerReports(): Map<String, ReportStock> = thirdPartyApi.daagerReports("https://tinvest.daager.ru/ostap-api/rd_data.json")

    suspend fun daagerIndices(): List<Index> = thirdPartyApi.daagerIndices("https://tinvest.daager.ru/ostap-api/indices.php")

    suspend fun daagerClosePrices(): Map<String, ClosePrice> = thirdPartyApi.daagerClosePrice("https://tinvest.daager.ru/ostap-api/close_alor2.json")

    suspend fun daagerShortInfo(): Map<String, StockShort> = thirdPartyApi.daagerShortInfo("https://tinvest.daager.ru/ostap-api/short.json")

    suspend fun daagerStockIndices(): StockIndexComponents = thirdPartyApi.daagerStockIndices("https://tinvest.daager.ru/ostap-api/gen/indices_components.json")

    suspend fun daagerStock1728(): Map<String, StockPrice1728> = thirdPartyApi.daagerStock1728("https://tinvest.daager.ru/ostap-api/gen/1600.json").payload

    suspend fun daagerMorningCompanies(): Map<String, Any> = thirdPartyApi.daagerMorningCompanies("https://tinvest.daager.ru/ostap-api/gen/morning_companies.json").payload

//    suspend fun daagerStocks(): Map<String, Instrument> = thirdPartyApi.daagerStocks("https://tinvest.daager.ru/tinkoff_stocks.json")

    suspend fun oostapTelegram(data: JsonObject): JsonObject {
        val json = thirdPartyApi.oostapTelegram("https://bot.oost.app:2358/", data)
        log("TELEGRAM response $json")
        return json
//        return json["tag_name"].toString().replace("\"", "")
    }

    suspend fun tinkoffPulse(ticker: String): Map<String, Any> {
        val data = thirdPartyApi.tinkoffPulse("https://api-invest-gw.tinkoff.ru/social/v1/post/instrument/$ticker?limit=30")
        return data.payload
    }

    suspend fun githubVersion(): String {
        val json = thirdPartyApi.githubVersion("https://api.github.com/repos/oostap/2358/releases/latest")
        return json["tag_name"].toString().replace("\"", "")
    }
}