# Coin Sniper ⚡

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/RichieLoco/BinanceNewCoinSniper/actions)
[![Java](https://img.shields.io/badge/java-21-blue)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.4.3-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/github/license/RichieLoco/BinanceNewCoinSniper)](LICENSE)

---

## 📈 Description

This is an **AI-enhanced crypto trading bot** that watches Binance for **new coin announcements** and uses **ChatGPT** to assess and select the best exchange to trade on. Inspired by:

* [Original Bot](https://github.com/CyberPunkMetalHead/new-listings-trading-bot)
* [Explanation Article](https://medium.com/coinmonks/i-made-an-open-source-trading-bot-that-trades-new-coin-listings-within-seconds-a529402b2cdd)

---

## 🧠 AI-Based Filtering

The bot uses **Spring AI** with OpenAI to assess exchanges by:

* ✅ **Latency** (API response time)
* 💧 **Liquidity**
* 💸 **Fees**

Filtering process:

```
Binance new coin → Find supporting exchanges → Score via ChatGPT → Pick lowest-risk → Trade
```

---

## ♻️ Functional Overview

* ✅ Poll Binance announcements
* ✅ Risk score exchanges via GPT
* ✅ Execute trade logic
* ✅ Store trade results
* ✅ Expose control via REST API
* ✅ View results via Web UI
* ✅ `/actuator/configprops` for runtime config
* ✅ Docker-ready for deployment

---

## 💠 API Endpoints

| Endpoint                             | Description              |
| ------------------------------------ | ------------------------ |
| `GET /api/announcements/call`        | Fetch announcements once |
| `GET /api/announcements/poll/status` | Show polling state       |
| `POST /api/announcements/poll/start` | Begin polling            |
| `POST /api/announcements/poll/stop`  | Stop polling             |

---

## 🌐 Thymeleaf Views

| Path               | Description                |
| ------------------ | -------------------------- |
| `GET /dashboard`   | View recent trades         |
| `GET /backtesting` | Visualize AI trade history |

Templates are in: `src/main/resources/templates/`

---

## ⚙️ Config (Actuator)

You can inspect active configuration at:

```
GET /actuator/configprops
```

Relevant classes:

* `AnnouncementPollingConfig`
* `CoinSniperConfig`
* `DashboardConfig`

Example YAML:

```yaml
announcement-polling:
  enabled: true
  interval-seconds: 60

coin-sniper:
  supported:
    exchanges:
      - Binance
      - Bybit
      - Kraken
  stable-coins:
    - USDT
    - USDC
```

---

## 💾 Persistence

Stored via **Spring Data R2DBC** in PostgreSQL/H2:

* Trades (coin, exchange, score, time)
* Errors (source, message, status)

---

## 🤖 Reinforcement Learning (DJL – planned)

The bot will use [DJL](https://djl.ai/) to:

* Learn from past trades
* Train a policy model
* Improve strategy over time

---

## 🥮 Testing

Includes:

* ✅ Unit tests for controllers
* ✅ Integration tests for API/database
* ✅ Mocked WebClient tests

Run:

```bash
mvn test
```

---

## 🐳 Docker

### Build image:

```bash
docker build -t coin-sniper .
```

### Run container:

```bash
docker run -p 8080:8080 \
  -e SPRING_AI_OPENAI_API_KEY=your_key \
  -e SPRING_PROFILES_ACTIVE=prod \
  coin-sniper
```

Visit:

* `http://localhost:8080/dashboard`
* `http://localhost:8080/backtesting`

---

## 🚀 Launch (Dev)

```bash
./mvnw spring-boot:run
```

---

## 📜 License

This project is licensed under the MIT License. See [`LICENSE`](LICENSE) for details.

---
