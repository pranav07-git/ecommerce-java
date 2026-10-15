# ShopHub – multi-role e-commerce platform

Spring Boot 3 / Java 17 · Thymeleaf · Spring Security · Spring Data JPA · H2 (file DB) · Chart.js

## Run

```bash
./mvnw spring-boot:run        # then open http://localhost:8080
./mvnw test                   # integration tests
```

Data persists in `./data/` (delete it to reset). Demo data is seeded on first start:

| Role   | Email             | Password  |
|--------|-------------------|-----------|
| Admin  | admin@shop.com    | admin123  |
| Seller | seller@shop.com   | seller123 |
| Seller | seller2@shop.com  | seller123 |
| Buyer  | buyer@shop.com    | buyer123  |

New buyers/sellers can self-register at `/register`; only admins can create admins.

## Features

**Admin** (`/admin`): user CRUD with roles (accounts that own products/orders are disabled rather than deleted), product CRUD for any seller, order list with status updates, live system-activity feed (polled every 3 s).

**Seller** (`/seller`): product listings (create/edit/delete), inventory updates with low-stock alerts and a stock chart, orders containing their products with status updates, sales reports (revenue by day, top products).

**Buyer** (`/`, `/buyer`): search/filter/sort products, cart and checkout (stock validated and decremented), order tracking with status timeline and cancellation (while `PLACED`), recently viewed products, wishlist with price-change tracking, profile, address book and payment-method labels.

Order flow: `PLACED → PROCESSING → SHIPPED → DELIVERED` (or `CANCELLED`, which restores stock). `DELIVERED`/`CANCELLED` are final.

## Layout

```
model/    JPA entities            service/  business rules + activity log
repo/     Spring Data repos       web/      controllers, forms, session cart
config/   security + data seeder  templates/ Thymeleaf pages per role
```

## Notes

- Removing a product is a soft delete so past orders stay intact.
- Payment methods store a display label only; there is no real payment gateway.
- To use another database, change `spring.datasource.*` in `application.properties` and add the driver.
