# Backend (Spring Boot 3.3, Java 17)

## Run
```bash
cp .env.example .env        # fill in values
mvn test                    # unit + web-slice tests
mvn spring-boot:run         # dev profile (default)
# production:
SPRING_PROFILES_ACTIVE=prod DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... java -jar target/*.jar
```
Swagger UI (dev only): `http://localhost:8080/swagger-ui.html`
Health: `/actuator/health` (liveness/readiness: `/actuator/health/liveness`, `/actuator/health/readiness`), metrics: `/actuator/prometheus`.

## API
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| GET | `/api/auth/me` | any user |
| GET | `/api/sales`, `/summary`, `/filters`, `/{id}` | any user |
| POST/PUT/DELETE | `/api/sales`, `/api/sales/{id}` | ADMIN |
| POST | `/api/forecasts` | any user |
| GET | `/api/forecasts/{product}?from=&to=`, `/{product}/latest` | any user |
| GET | `/api/analytics/overview`, `/trend`, `/top-products`, `/by-category`, `/by-region` | any user |
| GET | `/api/admin/users` | ADMIN |
| PATCH | `/api/admin/users/{id}/role` | ADMIN |

## Production notes
- `prod` profile has no fallback for `DB_*` / `JWT_SECRET`; the app refuses to start without them.
- The seeded `admin` user (migration V2) has a known password. In `prod` the app refuses to start until `ADMIN_PASSWORD` (min 8 chars) is set once; it then replaces the seeded password. In dev it only logs a warning.
- Login lockout is in-memory (per instance). With several replicas, move it to Redis or the gateway.
- `/actuator/prometheus` is unauthenticated; restrict it at the network level.
