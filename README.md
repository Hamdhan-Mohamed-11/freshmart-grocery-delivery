# FreshMart Grocery Delivery System

Hamdhan Mohamed Ameen · Kingston EUK00361749 · ESOFT E179021  
CI6115 Programming III · Coursework 2

A Java 21 CLI application with customer and administrator login, local persistence, inventory, delivery orders, cancellation and itemised invoices. No third-party Java libraries or database server are required.

## Run in VS Code

1. Install a **full JDK 21** and Microsoft's Extension Pack for Java in VS Code. A JRE alone cannot compile the project.
2. Extract the ZIP, open the **FreshMart** folder in VS Code and open Terminal → New Terminal.
3. Check `java -version` reports 21. Run one of the commands below from the project folder.

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

macOS or Linux:

```bash
bash run.sh
```

The scripts compile with the JDK's compiler module and launch the CLI. You can also use the included VS Code Run FreshMart launch configuration. Use the integrated terminal so password entry is hidden; redirected consoles display a warning that input will be visible.

## First run and use

Create your own administrator name, email and password (15–128 characters). No administrator password or real customer data is included. Log in, add products and stock, then log out. Register a customer with NIC, name, phone, address, email and password. Log in as that customer to browse/search, order, edit your profile, change your password, view history or cancel an eligible order.

For a quick demonstration add V1 Carrots, category Vegetables, price 1000, stock 20; add H1 Detergent, Household Essentials, price 6000, stock 20. Order V1 quantity 6 and H1 quantity 1 for tomorrow. The quote should be **LKR 12,425.00**. Cancel before dispatch and both stock quantities return to 20.

Administrator menu option 8 moves an order through PLACED → PREPARING → OUT_FOR_DELIVERY → DELIVERED. The customer can cancel in either of the first two states. Cancelled and delivered orders are terminal. Invoice option can export a text file to `invoices/`.

## Agreed business assumptions

- All prices are LKR. Unit prices are entered per product; category determines minimum, delivery, discount and tax.
- Mixed-category baskets are supported. The highest category minimum applies to the combined pre-discount product total. Charge the highest delivery fee once.
- Discount eligibility requires product total **strictly greater than 10000**. Apply the category discount to each qualifying line, then tax its discounted amount at its category rate. Delivery is untaxed.
- Round each line's discount and tax to two decimals using HALF_UP, then sum. Delivery is a single fixed fee.
- Quantity 0 = OUT_OF_STOCK, 1–5 = LOW_STOCK, 6+ = IN_STOCK. Only IN_STOCK products can be ordered, including when the requested quantity is small. Status is derived from quantity to prevent contradictory values.
- Reserve stock on order placement. Restore exactly once when an eligible cancellation succeeds. Retain products referenced by history; set stock to zero instead of deleting them.
- No same-day or past delivery. Dates use Asia/Colombo. Slots are 09:00–12:00, 12:00–16:00, 16:00–20:00. No delivery-capacity model.
- Customer NIC is the stable identity and cannot be edited. Email is unique, case insensitive, and used to log in. Profiles and passwords can be changed.
- Orders snapshot product and customer details so later edits do not rewrite invoices. Payment is assumed cash on delivery; this is not a payment processor.
- One local application process per data file. Not a multi-user web service.

| Category | Delivery | Minimum | Discount if eligible | Tax |
|---|---:|---:|---:|---:|
| Vegetables | 300 | 2000 | 5% | 5% |
| Fruits | 350 | 2500 | 5% | 5% |
| Dairy | 400 | 3000 | 8% | 8% |
| Household Essentials | 500 | 4000 | 10% | 10% |

## Verify

```powershell
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

```bash
bash test.sh
```

The suite creates temporary test directories and never touches your application data. Evidence in `docs/evidence/test-results.txt` records **63 passed, 0 failed** on OpenJDK 21.0.13, Linux. The same tests use a fixed date for deterministic boundaries. Windows/macOS scripts have been supplied but were not executed on those operating systems. Manually remove disposable `freshmart-test-*` directories from your OS temporary folder if desired.

## Data and recovery

Records live in `data/freshmart.db`. An optional first command-line argument selects a different file. Back up the database while the program is stopped. The CLI takes a file lock; do not bypass it by using symlink aliases or editing the file while running. Saves write a candidate snapshot to a temporary sibling, then replace the database; memory is updated only after a successful save. Atomic move is requested, with a normal replace fallback on filesystems that do not support it. The fallback is not guaranteed crash-atomic. A corrupted file causes startup to stop without replacing it: restore a known-good backup, rather than deleting the only copy.

Passwords use salted PBKDF2-HMAC-SHA256 with 600000 iterations. Base64 in the data format is **encoding, not encryption**; customer details remain readable to someone with filesystem access. Login does not provide encryption, account recovery, rate limiting or production-grade protection against a local machine administrator. Keep the folder private. Do not commit data, exported invoices, passwords or real NICs to GitHub.

## Project layout

- `src/freshmart/` — 18 application source files, each prefixed with the Kingston ID.
- `test/freshmart/` — executable regression suite.
- `docs/` — editable PlantUML diagrams, rendered diagrams and class signatures.
- `docs/evidence/` — actual CLI transcript, test log and readable captures of that output.
- `.vscode/` — Java source paths, integrated terminal and launch configuration.
- `GITHUB_SETUP.md` — instructions for publishing with your own GitHub account.

The report contains the design discussion, complete application and test source and Harvard references.
