# FarmLink – Spring Boot REST API

A farmer-to-retailer trading platform. This is a Java / Spring Boot rewrite of the backend of the
React + Supabase project *InfosysSpringboard-VirtualInternship*: everything Supabase did
(auth, database, row-level security, storage, edge functions, realtime notifications) is now plain Spring code.

**Stack:** Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA / Hibernate · MySQL 8 · Flyway ·
Bean Validation · springdoc (Swagger UI) · Razorpay · JavaMail · JUnit 5 / MockMvc

## Run it

1. Start MySQL 8 (any user that may create the `farmlink` database).
2. Set environment variables (PowerShell shown; only the DB password is really needed):

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:ADMIN_EMAIL = "admin@farmlink.com"        # creates the first admin on startup
$env:ADMIN_PASSWORD = "choose-a-strong-one"
$env:JWT_SECRET = "at-least-32-characters-long-random-string"
mvn spring-boot:run
```

3. Open Swagger UI: <http://localhost:8080/swagger-ui.html> → register, click **Authorize**, paste the token.

Flyway creates all tables on first start (`src/main/resources/db/migration/V1__init_schema.sql`).
Run the tests (no MySQL needed, they use in-memory H2): `mvn test`

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | MySQL connection | `localhost:3306/farmlink`, `root`, empty |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` | token signing / lifetime | dev secret, 1440 |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | seeds the first ADMIN | none |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | enables payments | none (payment endpoints answer 400) |
| `APP_MAIL_ENABLED=true`, `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD` | real emails | disabled: emails are only logged |
| `CORS_ORIGINS` | allowed frontend origins | localhost 5173/8081/3000 |

## API

All responses use `{ "status": "success|error", "data": ..., "message": ... }`. Send `Authorization: Bearer <token>`.

| Area | Endpoint | Who |
|---|---|---|
| Auth | `POST /api/auth/register` · `POST /api/auth/login` | public |
| Profile | `GET/PUT /api/users/me` | any user |
| Products | `GET /api/products` (`q`, `category`, `farmerId`, `page`, `size`, `sort`) · `GET /api/products/{id}` | public |
| | `GET /api/products/mine` · `POST` · `PUT /{id}` · `DELETE /{id}` · `POST /{id}/image` (multipart) | farmer |
| Orders | `POST /api/orders` (cart → one order per farmer) · `GET /api/orders/mine` | retailer |
| | `GET /api/orders/incoming` | farmer |
| | `GET /api/orders/{id}` · `PATCH /api/orders/{id}/status` | the two parties |
| Payments | `POST /api/payments/create-order` · `POST /api/payments/verify` | retailer |
| Reviews | `POST /api/reviews` | retailer (delivered orders only) |
| | `GET /api/farmers/{id}/reviews` · `GET /api/farmers/{id}/rating` | public |
| Notifications | `GET /api/notifications` · `GET /unread-count` · `PATCH /{id}/read` · `POST /read-all` | any user |
| Delivery | `GET /api/delivery/check?productId=&lat=&lng=` (distance, ETA, shelf-life warning) | any user |
| Farmer | `GET /api/farmers/me/analytics` | farmer |
| Admin | `GET /api/admin/users \| products \| orders \| reviews \| analytics` · `PATCH /users/{id}/verification` · `PATCH /products/{id}/availability` | admin |

**Order status rules:** farmer `PENDING→CONFIRMED|CANCELLED`, `CONFIRMED→SHIPPED|CANCELLED`, `SHIPPED→DELIVERED`;
retailer may only cancel a `PENDING` order. Confirming takes stock under a row lock; cancelling after confirmation returns it.

## How it maps from Supabase

| Supabase | Here |
|---|---|
| `auth.users` + `profiles` + `user_roles` | one `users` table with a `role` column |
| Row-level-security policies | `@PreAuthorize` roles + ownership checks in services |
| Edge functions `create-order` / `verify-payment` | `PaymentService` + `RazorpayGateway` (HMAC verified server-side) |
| Edge function `send-email` (Resend) | `EmailService` (JavaMail, async, after commit) |
| `notifications` table + realtime | `notifications` table + REST (poll `unread-count`) |
| Storage bucket `product-images` | `FileStorageService` → `/uploads/**` |
| React-side distance warning | `DeliveryService` (Haversine) |

## Deliberate improvements over the original

- Order totals and prices are computed on the server from the database, never trusted from the client.
- Payment amount is computed server-side; payment marks the order `PAID` and **does not** auto-confirm it, so stock is only taken when the farmer confirms (the original confirmed on payment and skipped the stock check).
- One Razorpay payment can cover several orders (a multi-farmer cart).
- Deleting a product that already has orders hides it instead of breaking order history.
- Enums are upper-case in JSON (`FARMER`, `PENDING`); query params and request bodies accept any case.

## Not included / next steps

- Refunds when a paid order is cancelled (needs the Razorpay refund API).
- Realtime push (SSE/WebSocket) – the frontend polls `/api/notifications/unread-count` for now.
- Pointing the original React app at this API (its `supabase.from(...)` calls would become `fetch('/api/...')`).
