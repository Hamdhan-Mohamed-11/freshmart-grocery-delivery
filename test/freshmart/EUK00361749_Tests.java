package freshmart;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FreshMart test cases, written by hand from the business rules of the case study.
 *
 * <p>Every case records the report's test-template columns: ID, area, type (Normal, Boundary or
 * Invalid), scenario, input and expected result. The body performs the scenario, checks the
 * expected result and returns the <em>actual</em> result it observed, which is printed in the
 * results table. All cases use a fixed clock (10 Nov 2026, Asia/Colombo) and temporary data files,
 * so they can be repeated on any day without touching real data.
 */
public final class EUK00361749_Tests {

    private static final char[] PASSWORD = "FreshMart demo passphrase".toCharArray();
    private static final String HASH = EUK00361749_Password.hash(PASSWORD);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T04:30:00Z"), EUK00361749_Service.STORE_ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final EUK00361749_TimeSlot MORNING = EUK00361749_TimeSlot.MORNING_09_12;
    private static final String CUSTOMER_NIC = "200012345678";

    /** The steps of one test case; returns a short description of the actual result. */
    @FunctionalInterface
    interface EUK00361749_Check {
        String run() throws Exception;
    }

    /** One row of the test-case table. */
    record EUK00361749_TestCase(String id, String area, String type, String scenario, String input,
                                String expected, EUK00361749_Check body) {
    }

    private static final List<EUK00361749_TestCase> CASES = new ArrayList<>();
    private static String currentArea = "";

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Fails the test unless the condition holds; otherwise returns the actual result text. */
    private static String expect(boolean condition, String actual) {
        if (!condition) {
            throw new AssertionError("Unexpected result: " + actual);
        }
        return actual;
    }

    /** Runs an action that must be refused and returns the refusal message as the actual result. */
    private static String rejected(EUK00361749_Check action) throws Exception {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
            return "Rejected: " + ex.getMessage();
        }
        throw new AssertionError("The action was accepted but should have been rejected.");
    }

    private static void sameMoney(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static EUK00361749_Product product(String id, EUK00361749_Category category, String price, int quantity) {
        return new EUK00361749_Product(id, "Demo " + id, category, new BigDecimal(price), quantity);
    }

    private static EUK00361749_Customer sampleCustomer() {
        return new EUK00361749_Customer(CUSTOMER_NIC, "Test Customer", "0771234567", "1 Test Road",
                "one@example.com", HASH);
    }

    private static EUK00361749_Store seed() {
        EUK00361749_Store store = new EUK00361749_Store();
        store.saveAccount(new EUK00361749_Admin("Test Admin", "admin@example.com", HASH));
        store.saveAccount(sampleCustomer());
        store.saveAccount(new EUK00361749_Customer("200112345678", "Other Customer", "0771234568", "2 Test Road",
                "two@example.com", HASH));
        store.saveProduct(product("V1", EUK00361749_Category.VEGETABLES, "1000", 20));
        store.saveProduct(product("F1", EUK00361749_Category.FRUITS, "2500", 20));
        store.saveProduct(product("D1", EUK00361749_Category.DAIRY, "3000", 20));
        store.saveProduct(product("H1", EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "4000", 20));
        return store;
    }

    private static Path newDataFile() throws IOException {
        return Files.createTempDirectory("freshmart-test-").resolve("freshmart.db");
    }

    /** A service over a seeded temporary data file, logged in as the first customer. */
    private static EUK00361749_Service fixture() throws Exception {
        Path file = newDataFile();
        seed().saveToFile(file);
        EUK00361749_Service service = new EUK00361749_Service(file, CLOCK, EUK00361749_PricingCatalogue.standard());
        loginCustomer(service);
        return service;
    }

    private static void loginCustomer(EUK00361749_Service service) {
        service.login("one@example.com", PASSWORD);
    }

    private static void loginAdmin(EUK00361749_Service service) {
        service.login("admin@example.com", PASSWORD);
    }

    /** Places V1 x2 for tomorrow morning (product total LKR 2,000). */
    private static EUK00361749_Order placeSmallOrder(EUK00361749_Service service) throws IOException {
        return service.placeOrder(Map.of("V1", 2), TOMORROW, MORNING);
    }

    private static int stockOf(EUK00361749_Service service, String productId) {
        return service.searchProducts(productId).get(0).stockQuantity();
    }

    private static EUK00361749_Order.EUK00361749_OrderBuilder builder() {
        return new EUK00361749_Order.EUK00361749_OrderBuilder(EUK00361749_PricingCatalogue.standard(), TODAY)
                .forCustomer(sampleCustomer())
                .deliverOn(TOMORROW)
                .inTimeSlot(MORNING);
    }

    /** Bill for one unit of a product in the given category, using the standard strategies. */
    private static EUK00361749_Invoice bill(EUK00361749_Category category, String amount) {
        return new EUK00361749_BillCalculator(EUK00361749_PricingCatalogue.standard())
                .calculate(List.of(new EUK00361749_OrderLine("X1", "Example", category, new BigDecimal(amount), 1)));
    }

    private static EUK00361749_OrderLine line(String id, EUK00361749_Category category, String amount) {
        return new EUK00361749_OrderLine(id, "Example " + id, category, new BigDecimal(amount), 1);
    }

    private static String minimumCheck(EUK00361749_Category category, String below, String exact,
                                       String expectedPayable) throws Exception {
        String refusal = rejected(() -> {
            builder().addProduct(product("X1", category, below, 10), 1).validate();
            return "";
        });
        builder().addProduct(product("X1", category, exact, 10), 1).validate();
        BigDecimal payable = bill(category, exact).finalPayable();
        sameMoney(expectedPayable, payable);
        return refusal + " | " + exact + " accepted, payable " + payable;
    }

    // =====================================================================
    // Registration and running
    // =====================================================================

    private static void area(String name) {
        currentArea = name;
    }

    private static void test(String type, String scenario, String input, String expected, EUK00361749_Check body) {
        String id = String.format("TC%02d", CASES.size() + 1);
        CASES.add(new EUK00361749_TestCase(id, currentArea, type, scenario, input, expected, body));
    }

    /** @return every test case in table order (used by the JUnit test factory). */
    static synchronized List<EUK00361749_TestCase> allCases() {
        if (CASES.isEmpty()) {
            defineCases();
        }
        return List.copyOf(CASES);
    }

    /**
     * Plain-Java runner that prints the results as the report's test table. The same cases also run
     * under JUnit 5 through {@link EUK00361749_JUnitTests}.
     */
    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        System.out.println("FreshMart test execution | Java " + System.getProperty("java.version")
                + " | fixed clock " + TODAY + " Asia/Colombo");
        System.out.println("ID | Area | Type | Scenario | Input | Expected result | Actual result | Status");
        for (EUK00361749_TestCase testCase : allCases()) {
            String actual;
            String status;
            try {
                actual = testCase.body().run();
                status = "PASS";
                passed++;
            } catch (Exception | AssertionError ex) {
                actual = String.valueOf(ex.getMessage());
                status = "FAIL";
                failed++;
            }
            System.out.println(String.join(" | ", testCase.id(), testCase.area(), testCase.type(),
                    testCase.scenario(), testCase.input(), testCase.expected(), actual, status));
        }
        System.out.println("RESULT: " + passed + " passed; " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // =====================================================================
    // Test cases, in the order of the report sections
    // =====================================================================

    private static void defineCases() {
        productAndInventoryCases();
        customerAccountCases();
        browsingAndSearchCases();
        placingOrderCases();
        historyAndCancellationCases();
        billingAndInvoiceCases();
        observerCases();
        persistenceAndDesignCases();
    }

    /** Report section 2.1: product and inventory management (administrator). */
    private static void productAndInventoryCases() {
        area("2.1 Products");
        test("Normal", "First-run administrator setup", "Empty data file; create administrator twice",
                "First accepted; second rejected", () -> {
                    EUK00361749_Service service =
                            new EUK00361749_Service(newDataFile(), CLOCK, EUK00361749_PricingCatalogue.standard());
                    boolean neededSetup = service.needsAdministratorSetup();
                    service.setupAdministrator("Admin", "boss@example.com", PASSWORD);
                    String second = rejected(() -> {
                        service.setupAdministrator("Admin", "x@example.com", PASSWORD);
                        return "";
                    });
                    return expect(neededSetup && !service.needsAdministratorSetup(), "Created; second " + second);
                });
        test("Normal", "Add then update a product", "Add Z1 Fruits LKR 3,000; update price to 3,500",
                "Z1 stored with price 3500.00", () -> {
                    EUK00361749_Service service = fixture();
                    loginAdmin(service);
                    service.addProduct(product("Z1", EUK00361749_Category.FRUITS, "3000", 8));
                    service.updateProduct(product("Z1", EUK00361749_Category.FRUITS, "3500", 10));
                    BigDecimal price = service.searchProducts("Z1").get(0).unitPrice();
                    sameMoney("3500", price);
                    return "Z1 price " + price;
                });
        test("Invalid", "Duplicate product ID", "Add V1 again", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            return rejected(() -> {
                service.addProduct(product("V1", EUK00361749_Category.FRUITS, "3000", 8));
                return "";
            });
        });
        test("Invalid", "Update unknown product", "Update Q9", "Rejected: not found", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            return rejected(() -> {
                service.updateProduct(product("Q9", EUK00361749_Category.FRUITS, "100", 8));
                return "";
            });
        });
        test("Normal", "Remove product without orders", "Remove F1", "F1 no longer found", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            service.removeProduct("F1");
            return expect(service.searchProducts("F1").isEmpty(), "F1 removed; search returns 0 results");
        });
        test("Invalid", "Remove product used in an order", "Order V1, then remove V1",
                "Rejected so old invoices stay valid", () -> {
                    EUK00361749_Service service = fixture();
                    placeSmallOrder(service);
                    loginAdmin(service);
                    return rejected(() -> {
                        service.removeProduct("V1");
                        return "";
                    });
                });
        test("Invalid", "Product field validation", "Price -1; stock -1; price 1.001; blank name",
                "All four rejected", () -> {
                    rejected(() -> product("X1", EUK00361749_Category.FRUITS, "-1", 8).toString());
                    rejected(() -> product("X1", EUK00361749_Category.FRUITS, "1", -1).toString());
                    rejected(() -> product("X1", EUK00361749_Category.FRUITS, "1.001", 8).toString());
                    rejected(() -> new EUK00361749_Product("X1", " ", EUK00361749_Category.FRUITS,
                            BigDecimal.ONE, 1).toString());
                    return "All four rejected";
                });
        test("Boundary", "Unit price limits", "0.01 and 1,000,000.00; then 1,000,000.01",
                "Limits accepted; above maximum rejected", () -> {
                    product("X1", EUK00361749_Category.FRUITS, "0.01", 8);
                    product("X1", EUK00361749_Category.FRUITS, "1000000.00", 8);
                    return "0.01 and 1,000,000.00 accepted; " + rejected(
                            () -> product("X1", EUK00361749_Category.FRUITS, "1000000.01", 8).toString());
                });
        test("Boundary", "Automatic stock status", "Quantities 0, 1, 5, 6",
                "Out of Stock, Low Stock, Low Stock, In Stock", () -> {
                    List<String> statuses = new ArrayList<>();
                    for (int quantity : new int[] {0, 1, 5, 6}) {
                        statuses.add(product("X1", EUK00361749_Category.FRUITS, "1", quantity).stockStatus().toString());
                    }
                    return expect(statuses.equals(List.of("Out of Stock", "Low Stock", "Low Stock", "In Stock")),
                            String.join(", ", statuses));
                });
        test("Normal", "Update stock quantity", "Set V1 quantity from 20 to 3", "Quantity 3; status Low Stock",
                () -> {
                    EUK00361749_Service service = fixture();
                    loginAdmin(service);
                    service.updateStockQuantity("V1", 3);
                    EUK00361749_Product updated = service.searchProducts("V1").get(0);
                    return expect(updated.stockQuantity() == 3
                                    && updated.stockStatus() == EUK00361749_StockStatus.LOW_STOCK,
                            "Quantity " + updated.stockQuantity() + "; " + updated.stockStatus());
                });
        test("Normal", "Set stock status manually", "Mark V1 Out of Stock (quantity 20); customer orders V1",
                "Quantity unchanged; order rejected", () -> {
                    EUK00361749_Service service = fixture();
                    loginAdmin(service);
                    service.updateStockStatus("V1", EUK00361749_StockStatus.OUT_OF_STOCK);
                    loginCustomer(service);
                    String refusal = rejected(() -> placeSmallOrder(service).orderId());
                    return expect(stockOf(service, "V1") == 20, "Quantity 20; " + refusal);
                });
        test("Invalid", "Contradictory stock status", "V1 quantity 0, then set In Stock", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            service.updateStockQuantity("V1", 0);
            return rejected(() -> {
                service.updateStockStatus("V1", EUK00361749_StockStatus.IN_STOCK);
                return "";
            });
        });
        test("Normal", "View inventory by category", "Category Dairy", "Only D1 listed", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            List<EUK00361749_Product> dairy = service.viewInventory(EUK00361749_Category.DAIRY);
            return expect(dairy.size() == 1 && dairy.get(0).productId().equals("D1"),
                    dairy.size() + " product: " + dairy.get(0).productId());
        });
        test("Normal", "Full inventory order (merge sort)", "All categories; products added out of order",
                "Sorted by category, then name", () -> {
                    EUK00361749_Service service = fixture();
                    loginAdmin(service);
                    service.addProduct(new EUK00361749_Product("V2", "Beans", EUK00361749_Category.VEGETABLES,
                            new BigDecimal("400"), 30));
                    List<String> ids = new ArrayList<>();
                    for (EUK00361749_Product item : service.viewInventory(null)) {
                        ids.add(item.productId());
                    }
                    return expect(ids.equals(List.of("V2", "V1", "F1", "D1", "H1")), "Order " + ids);
                });
        test("Invalid", "Customer uses an administrator function", "Customer adds a product", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> {
                service.addProduct(product("Z1", EUK00361749_Category.FRUITS, "3000", 8));
                return "";
            });
        });
    }

    /** Report section 2.2: customer registration and account maintenance. */
    private static void customerAccountCases() {
        area("2.2 Accounts");
        test("Normal", "Register a customer", "New NIC, name, phone, address, email, password",
                "Account created; can log in", () -> {
                    EUK00361749_Service service = fixture();
                    service.registerCustomer("199912345678", "New User", "0771111111", "5 New Street",
                            "new@example.com", PASSWORD);
                    String id = service.login("new@example.com", PASSWORD).getAccountId();
                    return expect(id.equals("199912345678"), "Logged in as NIC " + id);
                });
        test("Invalid", "Duplicate NIC", "Register NIC 200012345678 again", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> {
                service.registerCustomer(CUSTOMER_NIC, "Copy", "0771111111", "Road", "unique@example.com", PASSWORD);
                return "";
            });
        });
        test("Invalid", "Duplicate email", "ONE@EXAMPLE.COM (upper case)", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> {
                service.registerCustomer("199912345678", "Copy", "0771111111", "Road", "ONE@EXAMPLE.COM", PASSWORD);
                return "";
            });
        });
        test("Invalid", "Registration before setup", "Register on an empty system", "Rejected", () -> {
            EUK00361749_Service service =
                    new EUK00361749_Service(newDataFile(), CLOCK, EUK00361749_PricingCatalogue.standard());
            return rejected(() -> {
                service.registerCustomer("199912345678", "Early", "0771111111", "Road", "e@example.com", PASSWORD);
                return "";
            });
        });
        test("Invalid", "Invalid NIC", "ABC", "Rejected", () -> rejected(() -> EUK00361749_Validation.validateNic("ABC")));
        test("Boundary", "NIC formats", "123456789v (old) and 200012345678 (new)",
                "Both accepted; old format upper-cased", () -> {
                    String old = EUK00361749_Validation.validateNic("123456789v");
                    String modern = EUK00361749_Validation.validateNic("200012345678");
                    return expect(old.equals("123456789V"), old + " and " + modern);
                });
        test("Invalid", "Invalid email and phone", "\"bad\" and \"hello\"", "Both rejected", () ->
                rejected(() -> EUK00361749_Validation.validateEmail("bad")) + " | "
                        + rejected(() -> EUK00361749_Validation.validatePhone("hello")));
        test("Boundary", "Password length", "7 characters, then 8 characters", "7 rejected; 8 accepted", () -> {
            String refusal = rejected(() -> EUK00361749_Password.hash("Abcdefg".toCharArray()));
            EUK00361749_Password.hash("Abcdefgh".toCharArray());
            return refusal + " | 8 accepted";
        });
        test("Normal", "Login with any email case", "ONE@EXAMPLE.COM", "Customer logged in", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Account account = service.login("ONE@EXAMPLE.COM", PASSWORD);
            return expect(account instanceof EUK00361749_Customer, "Logged in: " + account.getName());
        });
        test("Invalid", "Wrong password", "Correct email, wrong password", "Rejected; nobody logged in", () -> {
            EUK00361749_Service service = fixture();
            String refusal = rejected(() -> service.login("one@example.com", "wrong passphrase".toCharArray()).getName());
            return expect(service.findLoggedInAccount() == null, refusal + "; session cleared");
        });
        test("Invalid", "Unknown email", "unknown@example.com", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.login("unknown@example.com", PASSWORD).getName());
        });
        test("Normal", "Edit profile", "New name, phone, address and email", "Details changed; NIC unchanged", () -> {
            EUK00361749_Service service = fixture();
            service.updateProfile("Updated Name", "0772222222", "9 New Address", "updated@example.com");
            EUK00361749_Account account = service.login("updated@example.com", PASSWORD);
            return expect(account.getAccountId().equals(CUSTOMER_NIC) && account.getName().equals("Updated Name"),
                    account.getName() + ", NIC " + account.getAccountId());
        });
        test("Invalid", "Profile email already used", "two@example.com", "Rejected; original email kept", () -> {
            EUK00361749_Service service = fixture();
            String refusal = rejected(() -> {
                service.updateProfile("Changed", "0772222222", "Road", "two@example.com");
                return "";
            });
            return expect(service.getLoggedInAccount().getEmail().equals("one@example.com"), refusal);
        });
        test("Normal", "Change password", "Correct current password + new password",
                "Old password refused; new accepted", () -> {
                    EUK00361749_Service service = fixture();
                    char[] next = "A different long password".toCharArray();
                    service.changePassword(PASSWORD, next);
                    String refusal = rejected(() -> service.login("one@example.com", PASSWORD).getName());
                    service.login("one@example.com", next);
                    return "Old " + refusal + " | new password accepted";
                });
        test("Invalid", "Change password with wrong current", "Current password wrong", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> {
                service.changePassword("wrong password".toCharArray(), PASSWORD);
                return "";
            });
        });
        test("Normal", "Salted password hashing", "Hash the same password twice",
                "Different hashes; both verify", () -> {
                    String other = EUK00361749_Password.hash(PASSWORD);
                    return expect(!other.equals(HASH) && EUK00361749_Password.verify(PASSWORD, HASH)
                            && EUK00361749_Password.verify(PASSWORD, other), "Hashes differ; both verify");
                });
    }

    /** Report section 2.3: browsing and searching products. */
    private static void browsingAndSearchCases() {
        area("2.3 Browse/search");
        test("Normal", "Browse by category", "Customer chooses Fruits", "Only F1 listed", () -> {
            EUK00361749_Service service = fixture();
            List<EUK00361749_Product> fruits = service.viewInventory(EUK00361749_Category.FRUITS);
            return expect(fruits.size() == 1 && fruits.get(0).productId().equals("F1"), "1 product: F1");
        });
        test("Normal", "Search by product ID", "v1 (lower case)", "Exactly V1", () -> {
            EUK00361749_Service service = fixture();
            List<EUK00361749_Product> found = service.searchProducts("v1");
            return expect(found.size() == 1 && found.get(0).productId().equals("V1"), "1 result: V1");
        });
        test("Normal", "Search by part of the name", "dEmO", "All four products", () -> {
            EUK00361749_Service service = fixture();
            int count = service.searchProducts("dEmO").size();
            return expect(count == 4, count + " results");
        });
        test("Invalid", "Search with no match", "absent", "Empty result", () -> {
            EUK00361749_Service service = fixture();
            return expect(service.searchProducts("absent").isEmpty(), "0 results");
        });
        test("Invalid", "Blank search text", "Spaces only", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.searchProducts("   ").toString());
        });
        test("Invalid", "Browse while logged out", "Log out, then view inventory", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            service.logout();
            return rejected(() -> service.viewInventory(null).toString());
        });
    }

    /** Report section 2.4 and 3.1: placing delivery orders through the Builder. */
    private static void placingOrderCases() {
        area("2.4 Orders (Builder)");
        test("Normal", "Place a valid order", "V1 x2, tomorrow, morning slot",
                "ORD0001 status Placed; stock 20 to 18", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    return expect(order.orderId().equals("ORD0001")
                                    && order.state() instanceof EUK00361749_PlacedState && stockOf(service, "V1") == 18,
                            order.orderId() + " " + order.state() + "; stock " + stockOf(service, "V1"));
                });
        test("Invalid", "Basket entry check", "V1 stock set to 5 (Low Stock); customer enters V1, then F1",
                "V1 refused at once; F1 accepted", () -> {
                    EUK00361749_Service service = fixture();
                    loginAdmin(service);
                    service.updateStockQuantity("V1", 5);
                    loginCustomer(service);
                    String refusal = rejected(() -> service.findOrderableProduct("V1").productId());
                    String accepted = service.findOrderableProduct("f1").productId();
                    return refusal + " | " + accepted + " accepted";
                });
        test("Invalid", "Order with no products", "Empty basket", "Rejected; no order saved", () -> {
            EUK00361749_Service service = fixture();
            String refusal = rejected(() -> service.placeOrder(Map.of(), TOMORROW, MORNING).orderId());
            return expect(service.viewOrderHistory().isEmpty(), refusal);
        });
        test("Invalid", "Zero or negative quantity", "V1 x0, then V1 x-1", "Both rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.placeOrder(Map.of("V1", 0), TOMORROW, MORNING).orderId()) + " | "
                    + rejected(() -> service.placeOrder(Map.of("V1", -1), TOMORROW, MORNING).orderId());
        });
        test("Invalid", "Unknown product", "Q9 x2", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.placeOrder(Map.of("Q9", 2), TOMORROW, MORNING).orderId());
        });
        test("Invalid", "Same product twice", "V1 x2 and v1 x2", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            Map<String, Integer> basket = new LinkedHashMap<>();
            basket.put("V1", 2);
            basket.put("v1", 2);
            return rejected(() -> service.placeOrder(basket, TOMORROW, MORNING).orderId());
        });
        test("Boundary", "Same-day delivery", "Delivery date 2026-11-10 (today)", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.placeOrder(Map.of("V1", 2), TODAY, MORNING).orderId());
        });
        test("Invalid", "Delivery date in the past", "2026-11-09", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.placeOrder(Map.of("V1", 2), TODAY.minusDays(1), MORNING).orderId());
        });
        test("Boundary", "Sri Lanka date, not UTC", "Clock 2026-11-10 20:00 UTC (11 Nov in Colombo); deliver 11 Nov",
                "Rejected as same-day", () -> {
                    Path file = newDataFile();
                    seed().saveToFile(file);
                    EUK00361749_Service service = new EUK00361749_Service(file,
                            Clock.fixed(Instant.parse("2026-11-10T20:00:00Z"), ZoneOffset.UTC),
                            EUK00361749_PricingCatalogue.standard());
                    loginCustomer(service);
                    return rejected(() -> placeSmallOrder(service).orderId());
                });
        test("Invalid", "No time slot chosen", "Time slot missing", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            return rejected(() -> service.placeOrder(Map.of("V1", 2), TOMORROW, null).orderId());
        });
        test("Invalid", "More than the stock", "V1 x21 (stock 20)", "Rejected; stock stays 20", () -> {
            EUK00361749_Service service = fixture();
            String refusal = rejected(() -> service.placeOrder(Map.of("V1", 21), TOMORROW, MORNING).orderId());
            return expect(stockOf(service, "V1") == 20, refusal);
        });
        test("Boundary", "Order the whole stock", "V1 x20 (stock 20)", "Accepted; V1 becomes Out of Stock", () -> {
            EUK00361749_Service service = fixture();
            service.placeOrder(Map.of("V1", 20), TOMORROW, MORNING);
            EUK00361749_Product after = service.searchProducts("V1").get(0);
            return expect(after.stockQuantity() == 0 && after.stockStatus() == EUK00361749_StockStatus.OUT_OF_STOCK,
                    "Stock 0; " + after.stockStatus());
        });
        test("Invalid", "Low Stock product", "V1 stock set to 5, order V1 x2", "Rejected (In Stock only)", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            service.updateStockQuantity("V1", 5);
            loginCustomer(service);
            return rejected(() -> placeSmallOrder(service).orderId());
        });
        test("Invalid", "Out of Stock product", "V1 stock set to 0, order V1 x2", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            service.updateStockQuantity("V1", 0);
            loginCustomer(service);
            return rejected(() -> placeSmallOrder(service).orderId());
        });
        test("Invalid", "One bad line stops the order", "V1 x2 plus F1 x99", "Rejected; no stock or history change",
                () -> {
                    EUK00361749_Service service = fixture();
                    Map<String, Integer> basket = new LinkedHashMap<>();
                    basket.put("V1", 2);
                    basket.put("F1", 99);
                    String refusal = rejected(() -> service.placeOrder(basket, TOMORROW, MORNING).orderId());
                    return expect(stockOf(service, "V1") == 20 && service.viewOrderHistory().isEmpty(), refusal);
                });
        test("Boundary", "Vegetables minimum order value", "LKR 1,999.99 then 2,000.00",
                "Below rejected; 2,000 accepted, payable 2,400", () ->
                        minimumCheck(EUK00361749_Category.VEGETABLES, "1999.99", "2000", "2400"));
        test("Boundary", "Fruits minimum order value", "LKR 2,499.99 then 2,500.00",
                "Below rejected; 2,500 accepted, payable 2,975", () ->
                        minimumCheck(EUK00361749_Category.FRUITS, "2499.99", "2500", "2975"));
        test("Boundary", "Dairy minimum order value", "LKR 2,999.99 then 3,000.00",
                "Below rejected; 3,000 accepted, payable 3,640", () ->
                        minimumCheck(EUK00361749_Category.DAIRY, "2999.99", "3000", "3640"));
        test("Boundary", "Household minimum order value", "LKR 3,999.99 then 4,000.00",
                "Below rejected; 4,000 accepted, payable 4,900", () ->
                        minimumCheck(EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "3999.99", "4000", "4900"));
        test("Invalid", "Mixed basket minimum", "Vegetables 2,000 + Household 1,000 (total 3,000)",
                "Rejected: highest minimum 4,000 applies", () -> rejected(() -> builder()
                        .addProduct(product("V9", EUK00361749_Category.VEGETABLES, "2000", 10), 1)
                        .addProduct(product("H9", EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "1000", 10), 1)
                        .validate().toString()));
        test("Invalid", "Builder without a customer", "No forCustomer step", "Rejected", () -> rejected(() ->
                new EUK00361749_Order.EUK00361749_OrderBuilder(EUK00361749_PricingCatalogue.standard(), TODAY)
                        .addProduct(product("V9", EUK00361749_Category.VEGETABLES, "2000", 10), 1)
                        .deliverOn(TOMORROW).inTimeSlot(MORNING).build("ORD0009").orderId()));
        test("Normal", "Builder creates the order", "Customer, V9 x2, D9 x1, tomorrow, evening",
                "Two lines, chosen slot, initial status Placed", () -> {
                    EUK00361749_Order order = builder()
                            .addProduct(product("V9", EUK00361749_Category.VEGETABLES, "1500", 10), 2)
                            .addProduct(product("D9", EUK00361749_Category.DAIRY, "500", 10), 1)
                            .inTimeSlot(EUK00361749_TimeSlot.EVENING_16_20)
                            .build("ORD0009");
                    return expect(order.lines().size() == 2 && order.state() instanceof EUK00361749_PlacedState
                                    && order.timeSlot() == EUK00361749_TimeSlot.EVENING_16_20,
                            order.lines().size() + " lines; " + order.timeSlot() + "; " + order.state());
                });
        test("Normal", "Price preview", "Quote V1 x2 for tomorrow", "Payable 2,400; nothing saved", () -> {
            EUK00361749_Service service = fixture();
            BigDecimal payable = service.quoteOrder(Map.of("V1", 2), TOMORROW, MORNING).finalPayable();
            sameMoney("2400", payable);
            return expect(stockOf(service, "V1") == 20 && service.viewOrderHistory().isEmpty(),
                    "Payable " + payable + "; stock 20; no order");
        });
        test("Invalid", "Administrator places an order", "Administrator logged in", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            loginAdmin(service);
            return rejected(() -> placeSmallOrder(service).orderId());
        });
        test("Normal", "Sequential order IDs", "Two orders", "ORD0001 then ORD0002", () -> {
            EUK00361749_Service service = fixture();
            String first = placeSmallOrder(service).orderId();
            String second = placeSmallOrder(service).orderId();
            return expect(first.equals("ORD0001") && second.equals("ORD0002"), first + ", " + second);
        });
    }

    /** Report section 2.5 and 3.4: order history and cancellation through the State pattern. */
    private static void historyAndCancellationCases() {
        area("2.5 History/cancel (State)");
        test("Normal", "Order history visibility", "Each customer orders once; customer and admin view history",
                "Customer sees 1 (own); administrator sees 2", () -> {
                    EUK00361749_Service service = fixture();
                    placeSmallOrder(service);
                    service.login("two@example.com", PASSWORD);
                    placeSmallOrder(service);
                    int own = service.viewOrderHistory().size();
                    loginAdmin(service);
                    int all = service.viewOrderHistory().size();
                    return expect(own == 1 && all == 2, "Customer " + own + "; administrator " + all);
                });
        test("Normal", "History sorted by delivery date", "ORD0001 for 14 Nov, ORD0002 for 11 Nov",
                "ORD0002 listed first", () -> {
                    EUK00361749_Service service = fixture();
                    service.placeOrder(Map.of("V1", 2), TODAY.plusDays(4), MORNING);
                    service.placeOrder(Map.of("V1", 2), TOMORROW, MORNING);
                    String first = service.viewOrderHistory().get(0).orderId();
                    return expect(first.equals("ORD0002"), "First listed " + first);
                });
        test("Normal", "Cancel a Placed order", "Place V1 x2, then cancel",
                "Status Cancelled; stock back to 20", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    service.cancelOrder(order.orderId());
                    EUK00361749_Order after = service.viewOrderHistory().get(0);
                    return expect(after.state() instanceof EUK00361749_CancelledState && stockOf(service, "V1") == 20,
                            after.state() + "; stock " + stockOf(service, "V1"));
                });
        test("Normal", "Cancel a Preparing order", "Advance once, then cancel", "Allowed; stock back to 20", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Order order = placeSmallOrder(service);
            loginAdmin(service);
            service.advanceOrderStatus(order.orderId());
            loginCustomer(service);
            service.cancelOrder(order.orderId());
            return expect(stockOf(service, "V1") == 20, "Cancelled from Preparing; stock 20");
        });
        test("Boundary", "Cancel once Out for Delivery", "Advance twice, then cancel",
                "Rejected; stock stays 18", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    service.advanceOrderStatus(order.orderId());
                    service.advanceOrderStatus(order.orderId());
                    loginCustomer(service);
                    String refusal = rejected(() -> {
                        service.cancelOrder(order.orderId());
                        return "";
                    });
                    return expect(stockOf(service, "V1") == 18, refusal + "; stock 18");
                });
        test("Invalid", "Cancel a Delivered order", "Advance three times, then cancel", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Order order = placeSmallOrder(service);
            loginAdmin(service);
            for (int i = 0; i < 3; i++) {
                service.advanceOrderStatus(order.orderId());
            }
            loginCustomer(service);
            return rejected(() -> {
                service.cancelOrder(order.orderId());
                return "";
            });
        });
        test("Invalid", "Cancel twice", "Cancel the same order again", "Second rejected; stock stays 20", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Order order = placeSmallOrder(service);
            service.cancelOrder(order.orderId());
            String refusal = rejected(() -> {
                service.cancelOrder(order.orderId());
                return "";
            });
            return expect(stockOf(service, "V1") == 20, refusal);
        });
        test("Normal", "Full delivery lifecycle", "Administrator advances ORD0001 three times, then once more",
                "Preparing, Out for Delivery, Delivered; 4th refused", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    List<String> states = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        states.add(service.advanceOrderStatus(order.orderId()).state().toString());
                    }
                    String refusal = rejected(() -> service.advanceOrderStatus(order.orderId()).orderId());
                    return expect(states.equals(List.of("Preparing", "Out for Delivery", "Delivered")),
                            String.join(" > ", states) + " | 4th " + refusal);
                });
        test("Invalid", "Advance a Cancelled order", "Cancel, then administrator advances", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Order order = placeSmallOrder(service);
            service.cancelOrder(order.orderId());
            loginAdmin(service);
            return rejected(() -> service.advanceOrderStatus(order.orderId()).orderId());
        });
        test("Invalid", "Customer changes delivery status", "Customer calls advance", "Rejected", () -> {
            EUK00361749_Service service = fixture();
            EUK00361749_Order order = placeSmallOrder(service);
            return rejected(() -> service.advanceOrderStatus(order.orderId()).orderId());
        });
        test("Invalid", "Another customer's order", "Customer 2 views invoice / cancels ORD0001",
                "History empty; both rejected", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    service.login("two@example.com", PASSWORD);
                    String invoice = rejected(() -> service.generateInvoice(order.orderId()));
                    String cancel = rejected(() -> {
                        service.cancelOrder(order.orderId());
                        return "";
                    });
                    return expect(service.viewOrderHistory().isEmpty(), invoice + " | " + cancel);
                });
        test("Normal", "State rules per status", "Ask each state whether it can be cancelled",
                "Placed, Preparing: yes; others: no", () -> {
                    List<EUK00361749_OrderState> states = List.of(new EUK00361749_PlacedState(),
                            new EUK00361749_PreparingState(), new EUK00361749_OutForDeliveryState(),
                            new EUK00361749_DeliveredState(), new EUK00361749_CancelledState());
                    StringBuilder actual = new StringBuilder();
                    for (EUK00361749_OrderState state : states) {
                        actual.append(state).append('=').append(state.isCancellable() ? "yes" : "no").append(' ');
                    }
                    String text = actual.toString().trim();
                    return expect(text.equals("Placed=yes Preparing=yes Out for Delivery=no Delivered=no Cancelled=no"), text);
                });
        test("Invalid", "Unknown saved status", "Status code SHIPPED", "Rejected", () ->
                rejected(() -> EUK00361749_OrderState.fromCode("SHIPPED").toString()));
    }

    /** Report section 2.6, 3.3 and 3.5: bill calculation, strategies, decorators and invoices. */
    private static void billingAndInvoiceCases() {
        area("2.6 Billing (Strategy/Decorator)");
        test("Boundary", "Discount threshold exactly LKR 10,000", "Vegetables 10,000.00",
                "No discount; payable 10,800", () -> {
                    EUK00361749_Invoice invoice = bill(EUK00361749_Category.VEGETABLES, "10000");
                    sameMoney("0", invoice.discount());
                    sameMoney("10800", invoice.finalPayable());
                    return "Discount " + invoice.discount() + "; payable " + invoice.finalPayable();
                });
        test("Boundary", "Just above the threshold", "Vegetables 10,000.01",
                "Discount 500; payable 10,275.01", () -> {
                    EUK00361749_Invoice invoice = bill(EUK00361749_Category.VEGETABLES, "10000.01");
                    sameMoney("500", invoice.discount());
                    sameMoney("10275.01", invoice.finalPayable());
                    return "Discount " + invoice.discount() + "; payable " + invoice.finalPayable();
                });
        test("Normal", "Dairy bill with discount", "Dairy 12,000",
                "Discount 960; delivery 400; tax 883.20; payable 12,323.20", () -> {
                    EUK00361749_Invoice invoice = bill(EUK00361749_Category.DAIRY, "12000");
                    sameMoney("960", invoice.discount());
                    sameMoney("400", invoice.deliveryFee());
                    sameMoney("883.20", invoice.tax());
                    sameMoney("12323.20", invoice.finalPayable());
                    return "Discount " + invoice.discount() + "; tax " + invoice.tax() + "; payable "
                            + invoice.finalPayable();
                });
        test("Normal", "Mixed-category bill", "Vegetables 6,000 + Household 6,000",
                "Discount 900; delivery 500; tax 825; payable 12,425", () -> {
                    EUK00361749_Invoice invoice = new EUK00361749_BillCalculator(EUK00361749_PricingCatalogue.standard())
                            .calculate(List.of(line("V9", EUK00361749_Category.VEGETABLES, "6000"),
                                    line("H9", EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "6000")));
                    sameMoney("900", invoice.discount());
                    sameMoney("500", invoice.deliveryFee());
                    sameMoney("825", invoice.tax());
                    sameMoney("12425", invoice.finalPayable());
                    return "Discount " + invoice.discount() + "; delivery " + invoice.deliveryFee() + "; tax "
                            + invoice.tax() + "; payable " + invoice.finalPayable();
                });
        test("Boundary", "Rounding of tax", "Vegetables 2,000.10 (tax 100.005)", "Tax 100.01 (half up)", () -> {
            BigDecimal tax = bill(EUK00361749_Category.VEGETABLES, "2000.10").tax();
            sameMoney("100.01", tax);
            return "Tax " + tax;
        });
        test("Normal", "Table 1 strategies", "Read each category strategy",
                "Fees 300/350/400/500; minimums 2,000/2,500/3,000/4,000", () -> {
                    EUK00361749_PricingCatalogue catalogue = EUK00361749_PricingCatalogue.standard();
                    List<String> fees = new ArrayList<>();
                    for (EUK00361749_Category category : EUK00361749_Category.values()) {
                        EUK00361749_PricingStrategy strategy = catalogue.strategyFor(category);
                        fees.add(strategy.getDeliveryFee().intValue() + "/" + strategy.getMinimumOrderValue().intValue());
                    }
                    return expect(fees.equals(List.of("300/2000", "350/2500", "400/3000", "500/4000")),
                            "fee/minimum " + fees);
                });
        test("Normal", "Swap a pricing strategy", "Dairy uses a 20% promotion strategy; Dairy 12,000",
                "Discount 2,400 with no billing-code change; standard still 960", () -> {
                    EUK00361749_PricingStrategy promotion = new EUK00361749_RatePricingStrategy("400", "3000", "0.20", "0.08") {
                    };
                    EUK00361749_PricingCatalogue promo =
                            EUK00361749_PricingCatalogue.standard().withStrategy(EUK00361749_Category.DAIRY, promotion);
                    EUK00361749_Invoice invoice = new EUK00361749_BillCalculator(promo)
                            .calculate(List.of(line("D9", EUK00361749_Category.DAIRY, "12000")));
                    sameMoney("2400", invoice.discount());
                    sameMoney("960", bill(EUK00361749_Category.DAIRY, "12000").discount());
                    return "Promotion discount " + invoice.discount() + "; standard discount 960.00";
                });
        test("Invalid", "Invalid strategy rate", "Discount rate 1.5 (150%)", "Rejected", () ->
                rejected(() -> new EUK00361749_RatePricingStrategy("400", "3000", "1.5", "0.08") {
                }.describe()));
        test("Normal", "Decorator layer order", "Build the bill for Dairy 12,000",
                "Product total > Discount > Delivery fee > Tax", () -> {
                    EUK00361749_Bill layered = new EUK00361749_BillCalculator(EUK00361749_PricingCatalogue.standard())
                            .buildBill(List.of(line("D9", EUK00361749_Category.DAIRY, "12000")));
                    return expect(layered.describeLayers().equals("Product total > Discount > Delivery fee > Tax"),
                            layered.describeLayers());
                });
        test("Normal", "Running amount after each layer", "Dairy 12,000 wrapped one layer at a time",
                "12,000 > 11,040 > 11,440 > 12,323.20", () -> {
                    EUK00361749_PricingCatalogue catalogue = EUK00361749_PricingCatalogue.standard();
                    EUK00361749_Bill step1 = new EUK00361749_ProductTotalBill(List.of(line("D9", EUK00361749_Category.DAIRY, "12000")));
                    EUK00361749_Bill step2 = new EUK00361749_DiscountDecorator(step1, catalogue);
                    EUK00361749_Bill step3 = new EUK00361749_DeliveryFeeDecorator(step2, catalogue);
                    EUK00361749_Bill step4 = new EUK00361749_TaxDecorator(step3, catalogue);
                    sameMoney("12000", step1.getAmount());
                    sameMoney("11040", step2.getAmount());
                    sameMoney("11440", step3.getAmount());
                    sameMoney("12323.20", step4.getAmount());
                    return step1.getAmount() + " > " + step2.getAmount() + " > " + step3.getAmount() + " > "
                            + step4.getAmount();
                });
        test("Normal", "Invoice contents", "Invoice for ORD0001 (V1 x2)",
                "Customer, product columns, all five bill amounts", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    String invoice = service.generateInvoice(order.orderId());
                    for (String required : List.of("Test Customer", "1 Test Road", "V1", "Demo V1", "Vegetables",
                            "1,000.00", "Product total", "Discount applied", "Delivery fee", "Tax added",
                            "FINAL PAYABLE AMOUNT", "LKR 2,400.00")) {
                        if (!invoice.contains(required)) {
                            throw new AssertionError("Invoice is missing " + required);
                        }
                    }
                    return "All 12 required items present; payable LKR 2,400.00";
                });
        test("Normal", "Invoice of a cancelled order", "Cancel ORD0001, then view invoice",
                "Marked cancelled; amount due LKR 0.00", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    service.cancelOrder(order.orderId());
                    String invoice = service.generateInvoice(order.orderId());
                    return expect(invoice.contains("CANCELLED ORDER") && invoice.contains("LKR 0.00 (cancelled)"),
                            "Header CANCELLED ORDER; due LKR 0.00");
                });
        test("Normal", "Invoice keeps order-time details", "Change profile and V1 price after ordering",
                "Invoice text unchanged", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    String before = service.generateInvoice(order.orderId());
                    service.updateProfile("New Name", "0771111111", "Changed Road", "one@example.com");
                    loginAdmin(service);
                    service.updateProduct(product("V1", EUK00361749_Category.VEGETABLES, "9999", 18));
                    return expect(service.generateInvoice(order.orderId()).equals(before), "Identical invoice text");
                });
    }

    /** Report section 3.2: Observer pattern. */
    private static void observerCases() {
        area("3.2 Observer");
        test("Normal", "Customer notified of status changes", "Place ORD0001; administrator advances it",
                "2 messages; inbox empty after reading", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_CustomerNotifier notifier = new EUK00361749_CustomerNotifier();
                    service.addOrderObserver(notifier);
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    service.advanceOrderStatus(order.orderId());
                    List<String> messages = notifier.collectMessages(CUSTOMER_NIC);
                    return expect(messages.size() == 2 && messages.get(1).contains("Placed to Preparing")
                                    && notifier.countMessages(CUSTOMER_NIC) == 0,
                            messages.size() + " messages; last: " + messages.get(1));
                });
        test("Normal", "Two observers on one subject", "Notifier and activity log; place, advance, cancel",
                "Both receive all 3 changes", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_CustomerNotifier notifier = new EUK00361749_CustomerNotifier();
                    EUK00361749_OrderActivityLog log = new EUK00361749_OrderActivityLog(CLOCK);
                    service.addOrderObserver(notifier);
                    service.addOrderObserver(log);
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    service.advanceOrderStatus(order.orderId());
                    loginCustomer(service);
                    service.cancelOrder(order.orderId());
                    int messages = notifier.countMessages(CUSTOMER_NIC);
                    List<String> entries = log.getEntries();
                    return expect(messages == 3 && entries.size() == 3 && entries.get(2).endsWith("Preparing        -> Cancelled"),
                            "Inbox " + messages + "; log " + entries.size() + " entries");
                });
        test("Invalid", "No notice for a refused change", "Cancel at Out for Delivery (refused)",
                "No new message", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_CustomerNotifier notifier = new EUK00361749_CustomerNotifier();
                    service.addOrderObserver(notifier);
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    service.advanceOrderStatus(order.orderId());
                    service.advanceOrderStatus(order.orderId());
                    loginCustomer(service);
                    rejected(() -> {
                        service.cancelOrder(order.orderId());
                        return "";
                    });
                    int count = notifier.countMessages(CUSTOMER_NIC);
                    return expect(count == 3, count + " messages (placed + 2 changes); none for the refusal");
                });
    }

    /** Persistence, data protection and object-oriented design checks. */
    private static void persistenceAndDesignCases() {
        area("Data and design");
        test("Normal", "Data kept after restart", "Place and advance ORD0001; reopen the data file",
                "Same order, status Preparing, stock 18", () -> {
                    Path file = newDataFile();
                    seed().saveToFile(file);
                    EUK00361749_Service service = new EUK00361749_Service(file, CLOCK, EUK00361749_PricingCatalogue.standard());
                    loginCustomer(service);
                    EUK00361749_Order order = placeSmallOrder(service);
                    loginAdmin(service);
                    service.advanceOrderStatus(order.orderId());
                    EUK00361749_Service reopened = new EUK00361749_Service(file, CLOCK, EUK00361749_PricingCatalogue.standard());
                    loginCustomer(reopened);
                    EUK00361749_Order loaded = reopened.viewOrderHistory().get(0);
                    return expect(loaded.orderId().equals("ORD0001") && loaded.state() instanceof EUK00361749_PreparingState
                                    && stockOf(reopened, "V1") == 18,
                            loaded.orderId() + " " + loaded.state() + "; stock " + stockOf(reopened, "V1"));
                });
        test("Normal", "No plain-text passwords", "Decode every saved field", "Password absent; PBKDF2 hash present",
                () -> {
                    Path file = newDataFile();
                    seed().saveToFile(file);
                    StringBuilder decoded = new StringBuilder();
                    for (String row : Files.readAllLines(file).subList(1, Files.readAllLines(file).size())) {
                        for (String field : row.split("\t")) {
                            decoded.append(new String(Base64.getDecoder().decode(field), StandardCharsets.UTF_8)).append('\n');
                        }
                    }
                    return expect(!decoded.toString().contains(new String(PASSWORD))
                            && decoded.toString().contains(EUK00361749_Password.ITERATIONS + ":"), "No plain text; hash stored");
                });
        test("Invalid", "Damaged data file", "File containing \"damaged\"", "Load refused; file unchanged", () -> {
            Path file = newDataFile();
            Files.writeString(file, "damaged");
            String refusal = rejected(() -> EUK00361749_Store.loadFromFile(file).toString());
            return expect(Files.readString(file).equals("damaged"), refusal);
        });
        test("Invalid", "Save failure", "Data file replaced by a folder; place order",
                "Rejected; stock and history unchanged in memory", () -> {
                    Path file = newDataFile();
                    seed().saveToFile(file);
                    EUK00361749_Service service = new EUK00361749_Service(file, CLOCK, EUK00361749_PricingCatalogue.standard());
                    loginCustomer(service);
                    Files.delete(file);
                    Files.createDirectory(file);
                    Files.writeString(file.resolve("block"), "keep");
                    rejected(() -> placeSmallOrder(service).orderId());
                    return expect(service.viewOrderHistory().isEmpty() && stockOf(service, "V1") == 20,
                            "Rejected (file could not be written); stock 20; no order in memory");
                });
        test("Normal", "Read-only lists", "Clear the returned inventory and order lines",
                "Both refuse changes", () -> {
                    EUK00361749_Service service = fixture();
                    EUK00361749_Order order = placeSmallOrder(service);
                    int refused = 0;
                    try {
                        service.viewInventory(null).clear();
                    } catch (UnsupportedOperationException expected) {
                        refused++;
                    }
                    try {
                        order.lines().clear();
                    } catch (UnsupportedOperationException expected) {
                        refused++;
                    }
                    return expect(refused == 2, "Both lists unmodifiable");
                });
        test("Normal", "Polymorphic accounts", "Admin and Customer held as Account",
                "Each returns its own menu title", () -> {
                    List<EUK00361749_Account> accounts = List.of(new EUK00361749_Admin("Boss", "boss@example.com", HASH),
                            sampleCustomer());
                    String admin = accounts.get(0).getMenuTitle();
                    String customer = accounts.get(1).getMenuTitle();
                    return expect(admin.startsWith("ADMINISTRATOR") && customer.startsWith("CUSTOMER"),
                            admin + " / " + customer);
                });
        test("Normal", "Generic merge sort", "[5, 3, 9, 1, 3]", "[1, 3, 3, 5, 9]; input unchanged", () -> {
            List<Integer> input = Arrays.asList(5, 3, 9, 1, 3);
            List<Integer> sorted = EUK00361749_Sorter.mergeSort(input, Comparator.naturalOrder());
            return expect(sorted.equals(List.of(1, 3, 3, 5, 9)) && input.equals(List.of(5, 3, 9, 1, 3)),
                    sorted.toString());
        });
    }
}
