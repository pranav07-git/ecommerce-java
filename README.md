# ShopHub - console e-commerce in plain Java

Terminal application using **only the JDK** (Java 17+): no frameworks, no build tool, no libraries.

## Run

```bash
./run.sh      # compile + start
./test.sh     # compile + run the self-test (34 checks)
```

Or manually: `javac -d out $(find src/main -name '*.java') && java -cp out com.ecommerce.Main`

Data is saved to `data/store.ser` after every change (delete the folder to reset). Demo data is created on first run:

| Role   | Email            | Password  |
|--------|------------------|-----------|
| Admin  | admin@shop.com   | admin123  |
| Seller | seller@shop.com  | seller123 |
| Seller | seller2@shop.com | seller123 |
| Buyer  | buyer@shop.com   | buyer123  |

Buyers and sellers can register from the main menu; only admins can create admins.

## Features

- **Admin:** list/create/edit/delete users and roles, list/add/edit/remove any product, list orders and update status, system activity log, and a live monitor that redraws every 2 s (Enter to exit).
- **Seller:** own listings (add/edit/remove), inventory updates, low-stock alerts and bar chart, orders containing their products with status updates, sales performance (revenue by day, top products as text charts).
- **Buyer:** search/filter/sort, product details, cart and checkout, order history and tracking timeline, cancel while `PLACED`, recently viewed, wishlist with price-change tracking, account (profile, address book, payment-method labels).

Order flow: `PLACED -> PROCESSING -> SHIPPED -> DELIVERED`, or `CANCELLED` (restores stock). `DELIVERED` and `CANCELLED` are final.

## Layout

```
src/main/java/com/ecommerce/
  Main.java      login / register loop
  model/         entities + Store (serialisable data holder)
  service/       Shop (all business rules), Seeder, SellerReport
  ui/            Console helpers, AdminMenu, SellerMenu, BuyerMenu
src/test/java/com/ecommerce/SelfTest.java
```

## Notes

- Passwords are stored as salted SHA-256 hashes.
- Removing a product is a soft delete, and deleting a user who has products or orders disables the account, so order history stays intact.
- Placing an order is all-or-nothing: stock is checked for every item before anything changes.
- Payment methods are display labels only; there is no payment gateway. Never enter real card numbers.
