# ExploreWithMe (EWM) Platform

ExploreWithMe – это платформа для поиска и организации мероприятий, управления заявками, рейтингов и аналитики, разработанная с использованием микросервисной архитектуры. Этот проект представляет собой модернизированную версию существующего монолитного приложения, которая подготовлена к высоким нагрузкам и масштабируемости.

---

## Архитектура

### Компоненты системы

1. **Discovery Server (Eureka)**:
    - Реализует сервис-ориентированное обнаружение.
    - Позволяет микросервисам регистрироваться и находить друг друга.

2. **Config Server**:
    - Централизованное хранилище конфигураций.
    - Упрощает управление настройками всех микросервисов.

3. **Gateway Server**:
    - API Gateway для маршрутизации запросов.
    - Инкапсулирует внутреннюю архитектуру, предоставляя единый входной API.

4. **Stats Service**:
    - Собирает и анализирует статистику по запросам пользователей.

5. **Event Service**:
    - Управляет событиями, категориями и компиляциями.

6. **Request Service**:
    - Управляет заявками на участие в мероприятиях.

7. **User Service**:
    - Обеспечивает управление профилями пользователей.

---

## Взаимодействие между сервисами

Микросервисы взаимодействуют друг с другом через:
- **OpenFeign** – для межсервисной коммуникации.
- **Eureka** – для динамического поиска сервисов.
- **API Gateway** – для маршрутизации внешних запросов к соответствующим сервисам.

Каждый микросервис обладает собственной базой данных на основе PostgreSQL и взаимодействует с другими сервисами через REST API.

---

## Конфигурация

Все конфигурации для микросервисов хранятся централизованно в **Config Server**. Взаимодействие настроено через Spring Cloud Config, который обеспечивает динамическое получение конфигурации.

---

## Этапы разработки

### Этап 1: Переход на облачную инфраструктуру
- Внедрение Spring Cloud Gateway, Eureka и Config Server.
- Централизованное управление конфигурацией и маршрутизацией запросов.

### Этап 2: Разделение на микросервисы
- Перенос функционала в отдельные модули (управление событиями, заявками, пользователями).
- Настройка маршрутизации и взаимодействия между сервисами через Feign и Eureka.
