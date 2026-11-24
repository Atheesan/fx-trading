# Authentication and user data

FXAI remains a Spring Boot modular monolith. Public market REST endpoints, health, and the market-only WebSocket stream remain public. Watchlist, alerts, and settings require a signed access token. No OAuth or social login is implemented.

## Configuration

Set `FXAI_AUTH_SIGNING_KEY` to Base64 encoding of at least 32 cryptographically random bytes. There is no production fallback. Generate it in PowerShell with `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`. Supply it through the environment or secret manager; never commit it. All backend instances must use the same key. Changing it invalidates existing access tokens; surviving refresh sessions can issue new tokens.

`FXAI_AUTH_SECURE_COOKIE` defaults to `true`. Production must use HTTPS. For local HTTP only, set it to `false` and run with the `local` Spring profile for credentialed CORS from the exact `FXAI_CORS_ALLOWED_ORIGIN` (default `http://localhost:5173`). Use `localhost` consistently for both frontend and backend, rather than mixing it with `127.0.0.1`. In production serve the frontend and `/api` through the same HTTPS origin and set `VITE_API_BASE_URL` to an empty string at frontend build time. The current strict SameSite cookies deliberately do not support a cross-site frontend/API deployment.

Apply Flyway V2 before serving traffic. It creates users, refresh sessions, watchlist and settings tables, and adds alert ownership. Existing unowned alerts remain stored but invisible through user APIs. Browser-local watchlists and themes are not automatically imported: a shared browser's old data has no proven account owner. New accounts start with the standard five-pair watchlist and the system theme.

## API

First `GET /api/v1/auth/csrf` with credentials included. Retain the returned `token` in memory and send it using the returned `headerName` (`X-XSRF-TOKEN`) on authentication POSTs and other browser mutations, along with the CSRF cookie. The CSRF cookie is HttpOnly; the client obtains its matching token from this response. Unsafe anonymous requests without a matching CSRF cookie/header are denied. Bearer-authenticated requests use Spring Security resource server CSRF handling; possession of the explicit Authorization header is required.

| Method | Endpoint | Body / response |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | `{ "email": "person@example.com", "password": "at-least-12-characters" }`; 201 with token response |
| POST | `/api/v1/auth/login` | Same fields; 200 with token response |
| POST | `/api/v1/auth/refresh` | Refresh cookie; 200 with token response and rotated refresh cookie |
| POST | `/api/v1/auth/logout` | Refresh cookie; revokes its session family, clears cookie, returns 204 |
| GET / PUT | `/api/v1/watchlist` | `{ "symbols": ["EURUSD"], "selectedSymbol": "EURUSD" }` |
| GET / PUT | `/api/v1/settings` | `{ "theme": "light" }`; allowed values: light, dark, system |

Token response: `accessToken`, `tokenType` (Bearer), `expiresIn` (600 seconds), and `userId`. Refresh tokens are never included in response JSON. Send `Authorization: Bearer <accessToken>` to user APIs. Auth responses use `Cache-Control: no-store`. Login failures and duplicate registrations return the same generic 401 authentication failure. Malformed inputs return generic 400 responses without rejected values or password details.

Email is case-normalized, validated, and limited to 254 characters. Registration passwords are 12-128 characters. Spring Security PBKDF2-HMAC-SHA256 uses a random 16-byte salt, 310,000 iterations, and a 256-bit result. Only salted password hashes are persisted. The missing-account login path performs the same password hash check against a dummy hash to reduce timing differences. New hashing parameters require a deliberate migration strategy.

Watchlists accept at most eight unique supported symbols, preserve order, and require the selected symbol to belong to the list. A null selection is permitted. All alert list/create/delete operations derive ownership from the verified JWT subject; request bodies cannot select another owner. Deleting another user's alert returns the same 404 as an unknown alert. Background market evaluation can still evaluate all users' active alerts without exposing them to other accounts.

## Token lifecycle and browser tradeoffs

Access tokens are HS256 JWTs issued for ten minutes, with issuer and expiry validation (Spring's normal clock-skew tolerance applies). They stay in JavaScript memory, never localStorage or sessionStorage. Refresh tokens contain 32 random bytes and are placed in a host-only HttpOnly, Secure, SameSite=Strict cookie scoped to `/api/v1/auth`. The database stores only their SHA-256 digests. They expire seven days after the original login, even after rotation. Each refresh is single use. Database locking serializes refresh/logout, and replay revokes the entire token family; replay revocation commits even though the request returns 401.

The frontend shares concurrent refresh requests within a tab, retries an unauthorized API call once, restores a session through the refresh cookie after reload, and clears cached account data when identity changes. Pending watchlist writes are serialized and checked against the current identity. A failed watchlist load cannot overwrite saved server data. Across tabs, simultaneous refreshes can trigger replay revocation and require signing in again. This intentionally favors replay detection over a refresh grace period.

Logout revokes refresh capability for that login session and clears browser memory/cookies. Already issued access tokens remain usable until their short expiry; there is no per-request access-token denylist or database lookup. Other device sessions remain signed in. Token theft through XSS is still possible while the page is active, and XSS can perform actions despite HttpOnly cookies. Deploy CSP, dependency hygiene, HTTPS, trusted same-origin hosting, and protect signing keys. HS256 shares signing authority across backend instances; asymmetric rotation can be considered later if deployment boundaries change.

Before public exposure configure authentication rate limits at the ingress/reverse proxy (both per-source and per-account), request body limits, and monitoring. Email verification, password reset, MFA, and OAuth are outside this implementation. Registration success versus rejection can still reveal whether an email is registered, despite generic failure wording. Periodically remove expired refresh_session rows only after their absolute expiry; retain used rows until then for replay detection. For example: `DELETE FROM refresh_session WHERE expires_at < now();`.

## Verification

Backend unit tests cover hashing and key validation; MVC security tests cover authentication, CSRF, and validation. PostgreSQL/TimescaleDB Testcontainers tests cover actual signed tokens, database migrations, login, rotation/replay, concurrent rotation, logout, expiry, and cross-user isolation. Frontend tests cover secure token handling, refresh sharing, watchlist persistence, and account flows. Run `./gradlew test` and `./gradlew build` from `backend` (Windows: `gradlew.bat`), plus `npm test` and `npm run build` from `frontend`. Tests use an isolated test-only signing key and HTTP cookies, never production environment secrets.

Reference: [Spring Security password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html) and [CSRF protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
