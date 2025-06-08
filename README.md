# 🚀 Coin Sniper

## 📝 Description

Based on similar trading logic written into the tool here: [CyberPunkMetalHead/new-listings-trading-bot](https://github.com/CyberPunkMetalHead/new-listings-trading-bot) (taken from the excellent article [here](https://medium.com/coinmonks/i-made-an-open-source-trading-bot-that-trades-new-coin-listings-within-seconds-a529402b2cdd)).

📈 History tells us that coins newly announced on Binance often enjoy a short-term spike in buying activity, causing a sharp rise in price! This bot aims to catch that momentum.

🧠 Unlike the original, this version introduces AI-based decision-making and supports a wide range of exchanges. It also reacts to delisting announcements by preparing to short the delisted coin.

## 🤖 AI-based Filtering

The bot polls Binance's Announcement API and then intelligently filters down potential exchanges based on multiple ChatGPT evaluations.

### 🔍 Filtering Breakdown

➡️ From:

```

<Configured supported exchanges>
```

➡️ To:

```
<Matched exchanges listing the newly announced Binance coin>
```

➡️ From:

```
<Multiple ChatGPT-based risk assessment calls on matched exchanges>
```

➡️ To:

```
<The single exchange we will trade with>
```

➡️ To:

```
<Persisted trade decision>
```

➡️ To:

```
<Reinforcement learning module input>
```

## 💾 Persistence Layer

The bot persists trade decisions and API responses into a database so it can track outcomes and serve them via:

* `/dashboard` (Thymeleaf UI)
* `/backtesting` (visual charts + history)
* `/actuator/configprops` (Spring Boot insights)

## 🧠 Deep Learning (Planned Feature)

Using [Deep Java Library (DJL)](https://djl.ai/), the bot will analyze historical trades and optimize future strategies through on-device reinforcement learning.

## 🔁 Continuous Polling

The app supports live polling of the Binance announcements endpoint. Use the API endpoints:

* `POST /api/announcements/poll/start` ➡️ Start polling
* `POST /api/announcements/poll/stop` ➡️ Stop polling
* `GET /api/announcements/poll/status` ➡️ Check polling status

## 🌐 Thymeleaf Dashboard Access

Access UI pages at:

* `http://localhost:8080/dashboard` ➡️ 📊 Recent trades
* `http://localhost:8080/backtesting` ➡️ 📉 Backtesting results chart

## 🧪 API & Swagger UI

Springdoc OpenAPI 3 integration allows exploration of endpoints via Swagger UI:

* `http://localhost:8080/swagger-ui.html` ➡️ 🔍 API explorer
* `http://localhost:8080/v3/api-docs` ➡️ 📃 OpenAPI JSON

## 🐳 Docker Deployment

Ensure environment variables are passed correctly. Build & run:

```bash
docker build -t coin-sniper .
docker run -e SPRING_PROFILES_ACTIVE=prod -e OPENAI_API_KEY=sk-xxxxxxxx -p 8080:8080 coin-sniper
```

## 🧩 Kubernetes Deployment

### 🛠️ Prerequisites:

* Java 21 JDK
* Docker or Podman
* Kubernetes cluster (e.g. k3s, kind, GKE)

### 🧰 Deployment Steps:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Visit: `http://<k8s-node-ip>:<nodePort>`

✅ Add `Ingress` and `Secret` for external TLS access as needed.

## 🍓 Raspberry Pi Deployment

### 📦 Steps:

```bash
# On Raspberry Pi
sudo apt install openjdk-21-jdk
java -jar coin-sniper.jar
```

You may also build for ARM:

```bash
./mvnw clean package
scp target/coin-sniper.jar pi@raspberrypi:/home/pi
```

## 🔧 Configuration Example (application.yml)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY} # 🔐 Loaded from environment variable
coin-sniper:
  supported:
    exchanges:
      - Binance
      - Bybit
  stable-coins:
      - USDT
      - USDC
  api:
    binance:
      announcement:
        base-url: https://www.binance.com/bapi/apex/v1/public/apex/cms/article/list/query
  polling:
    enabled: true
    interval-seconds: 30
```

### 🔐 Set Your API Key Securely

Set the `OPENAI_API_KEY` environment variable in your terminal or CI environment:

**Linux/macOS:**

```bash
export OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Windows CMD:**

```cmd
set OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Windows PowerShell:**

```powershell
$env:OPENAI_API_KEY = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

---

🧠 Built with Spring Boot WebFlux, R2DBC, DJL, Spring AI, and OpenAPI 3

💬 Suggestions and contributions welcome!