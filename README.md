# ShopHub – multi-role e-commerce platform

A server-rendered e-commerce web app with three roles (**Admin**, **Seller**, **Buyer**), each with its own dashboard.

Spring Boot 3.3 / Java 17 · Thymeleaf · Spring Security · Spring Data JPA · Bean Validation · Lombok · H2 (file DB) · Chart.js

## Requirements

- JDK 17+
- No Maven install needed: the Maven wrapper (`mvnw` / `mvnw.cmd`) is included

## Run

```bash
./mvnw spring-boot:run        # then open http://localhost:8080
./mvnw test                   # integration tests (in-memory H2)
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Data persists in `./data/` (git-ignored; delete it to reset). On first start with an empty database, demo users, six products in several categories and one sample order are seeded:

| Role   | Email             | Password  |
|--------|-------------------|-----------|
| Admin  | admin@shop.com    | admin123  |
| Seller | seller@shop.com   | seller123 |
| Seller | seller2@shop.com  | seller123 |
| Buyer  | buyer@shop.com    | buyer123  |

After login, each user is sent to the dashboard for their role. New buyers and sellers can sign up at `/register`; only admins can create admin accounts.

### H2 console

Open `/h2-console` (admins only) with JDBC URL `jdbc:h2:file:./data/ecommerce;AUTO_SERVER=TRUE`, user `sa` and an empty password.

## Features

**Admin** (`/admin`)
- User CRUD with roles. Accounts that own products or orders are disabled instead of deleted.
- Product CRUD for any seller.
- List of all orders, with status updates.
- Live system-activity feed (`/admin/activity`, polled every 3 s).

**Seller** (`/seller`)
- Product listings: create, edit and delete.
- Inventory updates with low-stock alerts (threshold set per product, default 5) and a stock chart.
- Orders that contain the seller's products, with status updates.
- Sales reports: revenue by day and top products.

**Buyer** (`/`, `/buyer`)
- Search, filter by category and sort the catalogue; product detail pages.
- Session cart and checkout. Stock is checked and reduced at checkout.
- Order tracking with a status timeline, and cancellation while the order is `PLACED`.
- Recently viewed products, and a wishlist that tracks price changes.
- Profile, address book and saved payment-method labels.

**Order flow:** `PLACED → PROCESSING → SHIPPED → DELIVERED`, or `CANCELLED`, which puts the stock back. `DELIVERED` and `CANCELLED` are final.

## Access rules

| Path                                                | Access   |
|-----------------------------------------------------|----------|
| `/`, `/products/**`, `/login`, `/register`, `/css/**` | Everyone |
| `/admin/**`, `/h2-console`                          | ADMIN    |
| `/seller/**`                                        | SELLER   |
| `/buyer/**`, `/cart/**`, `/checkout`, `/wishlist/**` | BUYER    |

## Project layout

```
src/main/java/com/ecommerce/
  EcommerceApplication.java   entry point
  config/    SecurityConfig (roles, login redirect), DataSeeder (demo data)
  model/     JPA entities: User, Product, Order, OrderItem, Address, PaymentMethod,
             WishlistItem, ViewHistory, ActivityLog; enums Role, OrderStatus
  repo/      Spring Data repositories
  service/   business rules: users, products, orders, buyer features, reports, activity log
  web/       controllers (Shop, Auth, Cart, Buyer, Seller, Admin), forms, session Cart, error handling
src/main/resources/
  templates/ Thymeleaf pages: shared (home, product, cart, login, register),
             admin/, seller/, buyer/, fragments/layout.html
  static/css/style.css
src/test/java/com/ecommerce/EcommerceFlowTest.java
```

## Tests

`EcommerceFlowTest` runs against an in-memory H2 database (`src/test/resources/application.properties`). It covers:
- public pages render, and role restrictions are enforced
- every role's dashboard renders
- the full flow from a buyer's purchase to the seller fulfilling it
- cancelling an order restores stock
- buyers cannot order more than is in stock
- admin user and product management

## Notes

- Removing a product is a soft delete, so past orders stay intact.
- Payment methods store a display label only; there is no real payment gateway.
- To use another database, change `spring.datasource.*` in `src/main/resources/application.properties` and add its JDBC driver to `pom.xml`.
