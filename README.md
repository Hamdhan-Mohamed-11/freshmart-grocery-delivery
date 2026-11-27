# FreshMart Grocery Delivery System

Hamdhan Mohamed Ameen | Kingston University ID EUK00361749 | ESOFT E179021
CI6115 Programming III – Patterns and Algorithms, Coursework 2 (updated brief)

A Java 17+ command-line application for product and stock administration, customer accounts,
delivery orders, order tracking and cancellation, and detailed invoices. It applies the five
design patterns required by the brief: **Builder, Observer, Strategy, State and Decorator**.
No framework, database or build tool is needed; JUnit 5 is included in `lib/` for the tests.

## Run

1. Install a JDK 17 or newer and the VS Code *Extension Pack for Java*.
2. Open the `FreshMart` folder in VS Code.
3. Windows PowerShell: `./run.ps1` &nbsp;|&nbsp; macOS/Linux: `bash run.sh`
   (or open `EUK00361749_Main.java` and click **Run** above `main`).

On the first run you create the administrator account (name, email, password of at least
8 characters). Then: log in as administrator → add products → log out → register a customer →
log in → place an order (basket, date, slot, price preview, confirm) → note the order ID (e.g.
`ORD0001`). Log in as administrator again to advance the order's status (option 9) and view the
activity log (option 11); the customer sees the notifications at their next login (option 9).

## Tests

Windows: `./test.ps1` &nbsp;|&nbsp; macOS/Linux: `bash test.sh`

The script compiles the project and runs 98 hand-written test cases (normal, boundary and invalid
inputs) with **JUnit 5** (`lib/junit-platform-console-standalone-1.11.4.jar`). Each case records
its area, type, scenario, input, expected result and the **actual** result it observed; the table
is saved to `docs/evidence/test-results.txt`. Cases use a fixed clock (10 Nov 2026, Asia/Colombo)
and temporary data files. In VS Code the tests also appear in the **Testing** panel (beaker icon).

## Design patterns

| Pattern | Classes | What it does in FreshMart |
|---|---|---|
| Builder | `Order.OrderBuilder` | Builds an order step by step (customer, products and quantities, date, slot) and validates every order rule in `validate()` before `build()` creates it with status Placed |
| Observer | `OrderObserver`, `Service` (subject), `CustomerNotifier`, `OrderActivityLog` | Every status change (placed, advanced, cancelled) is pushed to the customer inbox and the administrator's activity log |
| Strategy | `PricingStrategy`, `RatePricingStrategy`, `VegetablesPricing` … `HouseholdEssentialsPricing`, `PricingCatalogue` | Each category's delivery fee, minimum order value, discount and tax (Table 1) is an interchangeable strategy |
| State | `OrderState`, `PlacedState`, `PreparingState`, `OutForDeliveryState`, `DeliveredState`, `CancelledState`; `Order` is the context | The current state decides the next status and refuses cancellation from Out for Delivery onwards |
| Decorator | `Bill`, `ProductTotalBill`, `BillDecorator`, `DiscountDecorator`, `DeliveryFeeDecorator`, `TaxDecorator`, `BillCalculator` | The final bill is built in layers in the order of the brief: product total → discount → delivery fee → tax → final payable |

All class names carry the prefix `EUK00361749_`. The class diagram and flow chart are in `docs/`.

## Assumptions

- Table 1 gives one percentage per category under "Discount Rate | Tax Rate"; the same rate is used
  for the discount and the tax of that category (5%, 5%, 8%, 10%).
- An order must reach the highest minimum order value among the categories in the basket.
- One delivery fee is charged per order: the highest fee among its categories. It is not taxed.
- The category discount applies only when the product total **exceeds** LKR 10,000 and is
  calculated per line at that line's category rate. Tax is charged per line on the discounted value.
- Stock status follows the quantity (0 Out of Stock, 1–5 Low Stock, 6+ In Stock), but the
  administrator can also set it. "In Stock" or "Low Stock" with zero quantity is rejected.
- Only In Stock products can be ordered. Stock is reserved when an order is placed and returned if
  it is cancelled.
- Order lifecycle: Placed → Preparing → Out for Delivery → Delivered, or Cancelled. "Preparing" is
  the assumed intermediate status. Customers may cancel while Placed or Preparing. The
  administrator advances the status.
- Delivery must be tomorrow or later (Asia/Colombo date). Three time slots are offered.
- The NIC (old 9 digits + V/X, or new 12 digits) is the permanent customer ID; email is unique and
  used to log in. One administrator account is created on the first run.
- Products that appear in any order cannot be deleted (set stock to 0 instead).
- Payment is cash on delivery. Notifications are in-app and kept for the current session only.

## Data

Records are saved to `data/freshmart.db` after every change (a copy is written first and only then
becomes live); exported invoices go to `data/invoices/`. Passwords are stored only as salted
PBKDF2 hashes. The `data/` folder is git-ignored. Use fictional details only.

## Diagrams

`docs/FreshMart_Class_Diagram.png` (UML class diagram) and `docs/FreshMart_Flowchart_part1.png` /
`part2.png` (main algorithm) were drawn with the Python (Pillow) scripts in `docs/diagram-source/`.
`docs/evidence/` holds the test results and the recorded CLI walkthrough (`cli-inputs.txt` lists
the typed responses, `cli-walkthrough.txt` and `cli-restart.txt` the captured output).
