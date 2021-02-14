package com.project.ti2358.data.manager

import com.project.ti2358.BuildConfig
import com.project.ti2358.data.model.dto.Currency
import com.project.ti2358.data.model.dto.OperationType
import com.project.ti2358.data.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@KoinApiExtension
class WorkflowManager() : KoinComponent {
    private val stockManager: StockManager by inject()
    private val depositManager: DepositManager by inject()

    private val sandboxService: SandboxService by inject()

    private val marketService: MarketService by inject()
    private val ordersService: OrdersService by inject()

    fun startApp() {
        if (SettingsManager.isSandbox()) { // TEST
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    sandboxService.register()
                    sandboxService.setCurrencyBalance(Currency.USD, 10000)
                    val figi = marketService.searchByTicker("TSLA").instruments[0].figi
                    ordersService.placeMarketOrder(1, figi, OperationType.BUY)

                    // !!! в sandbox больше 1 лота нельзя покупать!
                    ordersService.placeMarketOrder(1, "BBG000BH5LT6", OperationType.BUY)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        stockManager.loadStocks()
        depositManager.startUpdatePortfolio()
    }

    companion object {

        private val processingModule = module {
            fun provideWorkflowManager(): WorkflowManager {
                return WorkflowManager()
            }

            fun provideStocksManager(): StockManager {
                return StockManager()
            }

            fun provideDepoManager(): DepositManager {
                return DepositManager()
            }

            fun provideStrategyPremarket(): StrategyPremarket {
                return StrategyPremarket()
            }

            fun provideStrategyPostmarket(): StrategyPostmarket {
                return StrategyPostmarket()
            }

            fun provideStrategy1000Sell(): Strategy1000Sell {
                return Strategy1000Sell()
            }

            fun provideStrategy1000Buy(): Strategy1000Buy {
                return Strategy1000Buy()
            }

            fun provideStrategy1005(): Strategy1005 {
                return Strategy1005()
            }

            fun provideStrategy2358(): Strategy2358 {
                return Strategy2358()
            }

            fun provideStrategy1728(): Strategy1728 {
                return Strategy1728()
            }

            fun provideStrategy1830(): Strategy1830 {
                return Strategy1830()
            }

            fun provideStrategyRocket(): StrategyRocket {
                return StrategyRocket()
            }

            fun provideStrategyTazik(): StrategyTazik {
                return StrategyTazik()
            }

            single { provideStocksManager() }
            single { provideDepoManager() }
            single { provideWorkflowManager() }

            single { provideStrategyPremarket() }
            single { provideStrategyPostmarket() }
            single { provideStrategy1000Sell() }
            single { provideStrategy1000Buy() }
            single { provideStrategy1005() }
            single { provideStrategy2358() }
            single { provideStrategy1728() }
            single { provideStrategy1830() }
            single { provideStrategyRocket() }
            single { provideStrategyTazik() }
        }

        private val apiModule = module {
            fun provideSandboxService(retrofit: Retrofit): SandboxService {
                return SandboxService(retrofit)
            }

            fun provideMarketService(retrofit: Retrofit): MarketService {
                return MarketService(retrofit)
            }

            fun provideOrdersService(retrofit: Retrofit): OrdersService {
                return OrdersService(retrofit)
            }

            fun providePortfolioService(retrofit: Retrofit): PortfolioService {
                return PortfolioService(retrofit)
            }

            fun provideOperationsService(retrofit: Retrofit): OperationsService {
                return OperationsService(retrofit)
            }

            fun provideStreamingService(): StreamingService {
                return StreamingService()
            }

            single { provideSandboxService(get()) }
            single { provideMarketService(get()) }
            single { provideOrdersService(get()) }
            single { providePortfolioService(get()) }
            single { provideOperationsService(get()) }
            single { provideStreamingService() }
        }

        private val retrofitModule = module {
            fun provideRetrofit(): Retrofit {
                var level = HttpLoggingInterceptor.Level.NONE
                if (BuildConfig.DEBUG) {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val httpClient = OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor())
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(level))
                    .build()

                return Retrofit.Builder()
                    .client(httpClient)
                    .baseUrl(SettingsManager.getActiveBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            single { provideRetrofit() }
        }

        fun startKoin() {
            org.koin.core.context.startKoin {
                modules(retrofitModule, apiModule, processingModule)
            }
        }
    }
}