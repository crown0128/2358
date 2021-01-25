# 2358
Helper for 2358 strategy via Tinkoff Invest OpenAPI

**Проект находится на этапе тестирования**

Приложение для Android.

Для работы приложения в настройках необходимо ввести торговый токен Tinkoff Invest OpenApi.
Токен используется только для авторизации в Tinkoff, никто, кроме вас, его не увидит.
Кусок кода, который отвечает за получение токена: SettingsManager::getActiveToken().
Этот токен используется только в двух местах: 
- AuthInterceptor для REST Api;
- StreamingService: для Streaming Api.

**Фичи:**
- 2358: настроить таймер на покупку по рынку выбранных бумаг в заданное время;
- 1000: настроить таймер на создание заявки на продажу в 10:00:01 выбранных бумаг по заданной цене;

**Особенности:**
- работает в фоновом режиме;
- работает только с $;
- время в настройках в часовом поясе МСК;
- время в уведомлениях в локальном часовом поясе;
- изменение цены измеряется по дневной свече.

**Настройки:**
- фильтры: по цене, объёму, изменению цены
- % профита
- сумма покупки

**Сценарий первого старта:**
- запуск;
- в настройках выключаем Sandbox режим и вводим Торговый токен;
- перезагружаем приложение.

**Сценарий использования 2358:**
За несколько часов до конца ОС запускаем приложение, в настройках задаём нужные параметры (по умолчанию заданы базовые),
переходим на экран 2358, жмём 'обновить', выбираем нужные бумаги для покупки, жмём 'продолжить' (по клику на строку можно перейти в Тинькофф),
проверяем сводку, жмём 'старт', в шторке создаётся уведомление с таймером на покупку. Для отмены таймера нужно нажать 'стоп' в уведомлении.

**Сценарий тестирования 2358:**
Выбрать в настройках время покупки через 5 минут, выбрать бюджет 10$, изменение цены задать 0%. На экране 2358 выбрать мусор, 'продолжить' -> 'старт'.

**Сценарий использования 1000:**
До старта премаркета запускаем приложение, переходим на экран 1000, жмём 'обновить', выбираем нужные бумаги из портфеля, для которых нужно создать заявку
на продажу в 10:00:01 по заданной цене, жмём 'продолжить', проверяем сводку, настраиваем цену продажи для каждой бумаги, жмём 'старт',
в шторке создаётся уведомление с таймером на продажу. Для отмены таймера нужно нажать 'стоп' в уведомлении.


# ВНИМАНИЕ!
В приложении отсутствует кнопка 'Бабло', ответственность за ваши действия лежит на вас, 
проект представляет собой простой таймер, если вы не понимаете его назначение, лучше им не пользуйтесь.
