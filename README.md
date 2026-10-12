# FXAI

## Forex market-data provider

FXAI keeps vendor APIs behind `MarketDataProvider`. The default provider is deterministic mock data, so local development and automated tests do not need credentials or network access.

Select a provider with `FXAI_MARKET_PROVIDER`:

```text
FXAI_MARKET_PROVIDER=mock
```

or:

```text
FXAI_MARKET_PROVIDER=oanda
OANDA_API_TOKEN=your-practice-api-token
OANDA_ACCOUNT_ID=your-practice-account-id
```

The OANDA adapter targets the v20 Practice REST API at `https://api-fxpractice.oanda.com`. Create a Practice account and personal access token in OANDA, then provide the token and account ID through environment variables. Never put real values in `.env.example` or commit a populated `.env` file. `OANDA_BASE_URL`, `OANDA_CONNECTION_TIMEOUT`, and `OANDA_REQUEST_TIMEOUT` can optionally override the Practice URL and the default `2s`/`5s` timeouts.

### Symbol and timeframe mapping

| FXAI | OANDA |
|---|---|
| `EURUSD` | `EUR_USD` |
| `GBPUSD` | `GBP_USD` |
| `USDJPY` | `USD_JPY` |
| `AUDUSD` | `AUD_USD` |
| `NZDUSD` | `NZD_USD` |
| `USDCAD` | `USD_CAD` |
| `USDCHF` | `USD_CHF` |
| `EURGBP` | `EUR_GBP` |

| FXAI timeframe | OANDA granularity |
|---|---|
| `1m` | `M1` |
| `5m` | `M5` |
| `15m` | `M15` |
| `1h` | `H1` |
| `1d` | `D` |

Current pricing is available at `GET /api/v1/market/prices/{symbol}`. Historical midpoint candles are available at `GET /api/v1/market/candles/{symbol}?timeframe=1h&limit=500`. Supported timeframes are `1m`, `5m`, `15m`, `1h`, and `1d`. Optional `from` and `to` parameters must be ISO-8601 UTC timestamps, for example `2026-01-01T00:00:00Z`. Every request has a bounded result size: `limit` defaults to 500 and must be between 1 and 5000. OANDA DTOs stay inside the adapter and are mapped to FXAI domain objects before reaching REST controllers. The adapter performs REST polling only—order placement, trading, and streaming are intentionally excluded. Its REST client is isolated so a future `OandaPricingStreamClient` can be added without changing the domain provider contract.

### Real-time prices

The backend has one centralized price poller for all connected browsers. It reads each supported pair through `MarketDataProvider`, publishes internal quote events, and broadcasts `PRICE_UPDATE` messages from `/ws/market`. Browsers use WebSocket updates to refresh their shared query cache and do not independently poll the external Forex provider. Configure the backend polling delay in milliseconds with `FXAI_PRICE_POLL_INTERVAL`; it defaults to `15000`.

FXAI V1 is a real-time Forex market monitoring dashboard and a learning project for modern full-stack, event-driven, cloud-native engineering.

## Repository layout

```text
.
├── backend/         Spring Boot and Gradle application
├── frontend/        React, TypeScript, and Vite application
├── infrastructure/  Future Docker Compose and observability configuration
└── docs/            Architecture and project documentation
```

See [the architecture document](docs/architecture.md) for V1 boundaries and planned flows.

## Backend

The backend uses a Gradle Java 25 LTS toolchain. The Gradle wrapper downloads the required Gradle version.

```shell
cd backend
./gradlew test
./gradlew bootRun
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

To run with local browser CORS enabled:

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

The local frontend origin defaults to `http://localhost:5173` and can be changed with `FXAI_CORS_ALLOWED_ORIGIN`.

### Market-data providers

See [Forex market-data provider](#forex-market-data-provider) for mock and OANDA Practice setup. On PowerShell, set the documented variables with `$env:VARIABLE = 'value'` before running `gradlew.bat bootRun`. Spring reads exported environment variables and does not require a checked-in `.env` file.

## Frontend

Requirements: a current Node.js release and npm.

```shell
cd frontend
npm install
npm run dev
```

Run `npm run build`, `npm test`, and `npm run lint` for validation.
