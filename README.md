# FreshMart Grocery Delivery System

Hamdhan Mohamed Ameen | Kingston University ID EUK00361749 | ESOFT E179021
CI6115 Programming III – Patterns and Algorithms, Coursework 2

A Java 17+ command-line application for product and stock administration, customer accounts,
delivery orders, cancellations and detailed invoices. No external libraries, database or build
tool are needed.

## Run

1. Install a JDK 17 or newer and the VS Code *Extension Pack for Java*.
2. Open the `FreshMart` folder in VS Code.
3. Windows PowerShell: `./run.ps1` &nbsp;|&nbsp; macOS/Linux: `bash run.sh`
   (or open `EUK00361749_Main.java` and click **Run** above `main`).

On the first run you create the administrator account (name, email, password of at least
8 characters). Then: log in as administrator → add products → log out → register a customer →
log in → place an order (basket, date, slot, preview, confirm) → note the order ID (e.g. `ORD0001`).
Log in as administrator again to advance the order's status; the customer sees a notification at
their next login.

## Tests

Windows: `./test.ps1` &nbsp;|&nbsp; macOS/Linux: `bash test.sh`

The script compiles the project and runs 71 automated test cases (normal, boundary and invalid
inputs) with **JUnit 5** (`lib/junit-platform-console-standalone-1.11.4.jar`, included). Each row
of the report's test table (T01–T71) is its own JUnit test, created by a `@TestFactory` in
`EUK00361749_JUnitTests`. The cases use a fixed clock (10 Nov 2026) and temporary data files. The
script then saves the same cases as a results table to `docs/evidence/test-results.txt`.

In VS Code the tests also appear in the **Testing** panel (beaker icon), because
`.vscode/settings.json` adds the JUnit jar to the project.

## Design summary

| Concept | Where |
|---|---|
| Inheritance / abstraction | `Account` (abstract) → `Admin`, `Customer` |
| Polymorphism | `getRole()` / `getMenuTitle()` overridden; `PricingPolicy` and `OrderObserver` interfaces |
| Encapsulation | private fields, immutable records, store returns unmodifiable copies |
| Strategy pattern | `PricingPolicy` ← `CategoryPricing` |
| Observer pattern | `Service` (subject) notifies `OrderObserver`s; `CustomerNotifier` keeps each customer's inbox |
| Builder pattern | `Order.OrderBuilder` assembles lines, date and slot before validation |
| Data structures | `LinkedHashMap` (O(1) lookup by ID), `HashSet` (duplicate line check), `HashMap` of message lists, `ArrayList` |
| Algorithms | generic merge sort (`Sorter`), linear product search, 5-step billing, sequential ID generation |

Class diagram source: `docs/class-diagram.puml` (render with the VS Code PlantUML extension or
plantuml.com).

## Assumptions

- An order must reach the highest minimum order value among the categories in the basket.
- One delivery fee is charged per order: the highest fee among its categories.
- The category discount applies only when the product total **exceeds** LKR 10,000, and is
  calculated per line at that line's category rate. Tax is charged per line on the discounted
  value; the delivery fee is not taxed.
- Stock status is calculated from quantity (0 Out of Stock, 1–5 Low Stock, 6+ In Stock), but the
  administrator can also set it directly. "In Stock" or "Low Stock" with zero quantity is rejected.
- Only In Stock products can be ordered (Low Stock items are held back).
- Stock is reserved when an order is placed and returned if it is cancelled.
- Customers can cancel while an order is Placed or Preparing (not once Out for Delivery).
- Delivery must be tomorrow or later (Asia/Colombo date).
- The NIC is the permanent customer ID; email is unique and used to log in.
- Products that appear in any order cannot be deleted (set stock to 0 instead).
- Payment is cash on delivery; no card processing.
- Notifications are kept for the current program session only.

## Data

Records are saved to `data/freshmart.db` after every change; exported invoices go to
`data/invoices/`. Passwords are stored only as salted PBKDF2 hashes. The `data/` folder is
git-ignored. Use fictional details only.
