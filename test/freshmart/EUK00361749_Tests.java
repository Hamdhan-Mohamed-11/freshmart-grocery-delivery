package freshmart;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Dependency-free regression suite; all fixtures use disposable directories. */
public final class EUK00361749_Tests {
  private static final char[] PASSWORD = "FreshMart demo passphrase".toCharArray();
  private static final String HASH = EUK00361749_Password.hash(PASSWORD);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), EUK00361749_Service.STORE_ZONE);
  private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 11);
  private static int passed, failed;

  @FunctionalInterface
  private interface EUK00361749_Check {
    void run() throws Exception;
  }

  private static void check(boolean value) {
    if (!value) throw new AssertionError("Condition false");
  }

  private static void rejects(EUK00361749_Check check) throws Exception {
    try {
      check.run();
    } catch (IllegalArgumentException | IOException ex) {
      return;
    }
    throw new AssertionError("Expected rejection");
  }

  private static void money(String expected, BigDecimal actual) {
    if (new BigDecimal(expected).compareTo(actual) != 0)
      throw new AssertionError("Expected " + expected + ", got " + actual);
  }

  private static EUK00361749_Product product(
      String id, EUK00361749_Category category, String price, int quantity) {
    return new EUK00361749_Product(id, "Demo " + id, category, new BigDecimal(price), quantity);
  }

  private static EUK00361749_Store seed() {
    EUK00361749_Store s = new EUK00361749_Store();
    s.saveAccount(new EUK00361749_Admin("Test Admin", "admin@example.com", HASH));
    s.saveAccount(
        new EUK00361749_Customer(
            "200012345678", "Test Customer", "0771234567", "1 Test Road", "one@example.com", HASH));
    s.saveAccount(
        new EUK00361749_Customer(
            "200112345678",
            "Other Customer",
            "0771234568",
            "2 Test Road",
            "two@example.com",
            HASH));
    s.saveProduct(product("V1", EUK00361749_Category.VEGETABLES, "1000", 20));
    s.saveProduct(product("F1", EUK00361749_Category.FRUITS, "2500", 20));
    s.saveProduct(product("D1", EUK00361749_Category.DAIRY, "3000", 20));
    s.saveProduct(product("H1", EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "4000", 20));
    return s;
  }

  private static Path path() throws IOException {
    return Files.createTempDirectory("freshmart-test-").resolve("db");
  }

  private static EUK00361749_Service fixture() throws Exception {
    Path p = path();
    seed().saveToFile(p);
    EUK00361749_Service s = new EUK00361749_Service(p, CLOCK, new EUK00361749_CategoryPricing());
    customer(s);
    return s;
  }

  private static void customer(EUK00361749_Service s) {
    s.login("one@example.com", PASSWORD);
  }

  private static void admin(EUK00361749_Service s) {
    s.login("admin@example.com", PASSWORD);
  }

  private static EUK00361749_Order order(EUK00361749_Service s) throws IOException {
    return s.placeOrder(Map.of("V1", 2), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12);
  }

  private static EUK00361749_Invoice price(EUK00361749_Category c, String amount) {
    return new EUK00361749_CategoryPricing()
        .calculate(
            List.of(new EUK00361749_OrderLine("X", "Example", c, new BigDecimal(amount), 1)));
  }

  private static void test(String name, String input, String expected, EUK00361749_Check body) {
    try {
      body.run();
      passed++;
      System.out.println(
          String.format("T%02d | %s | %s | %s | PASS", passed + failed, name, input, expected));
    } catch (Exception | AssertionError e) {
      failed++;
      System.out.println(
          String.format(
              "T%02d | %s | %s | %s | FAIL %s", passed + failed, name, input, expected, e));
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println(
        "FreshMart regression execution | Java "
            + System.getProperty("java.version")
            + " | "
            + Instant.now());
    System.out.println("ID | Function | Input | Expected and verified outcome | Result");
    test(
        "First-run setup",
        "New file; create admin twice",
        "First succeeds; duplicate rejected",
        () -> {
          EUK00361749_Service s =
              new EUK00361749_Service(path(), CLOCK, new EUK00361749_CategoryPricing());
          check(s.needsAdministratorSetup());
          s.setupAdministrator("Admin", "new@example.com", PASSWORD);
          check(!s.needsAdministratorSetup());
          rejects(() -> s.setupAdministrator("Admin", "a@b.com", PASSWORD));
        });
    test(
        "Login success",
        "Uppercase customer email",
        "Customer authenticated",
        () -> {
          EUK00361749_Service s = fixture();
          check(s.login("ONE@EXAMPLE.COM", PASSWORD) instanceof EUK00361749_Customer);
        });
    test(
        "Wrong password",
        "Incorrect password",
        "Rejected and previous session cleared",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.login("one@example.com", "incorrect passphrase".toCharArray()));
          rejects(s::getLoggedInAccount);
        });
    test(
        "Unknown email",
        "unknown@example.com",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.login("unknown@example.com", PASSWORD));
        });
    test(
        "Guest access",
        "Logout then browse",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          s.logout();
          rejects(() -> s.viewInventory(null));
        });
    test(
        "Role isolation",
        "Customer adds product",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.addProduct(product("Z", EUK00361749_Category.FRUITS, "3000", 8)));
        });
    test(
        "Admin order restriction",
        "Admin places order",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          rejects(() -> order(s));
        });
    test(
        "Register account",
        "New NIC and email",
        "New customer can log in",
        () -> {
          EUK00361749_Service s = fixture();
          s.registerCustomer(
              "199912345678", "New User", "0771111111", "New Street", "new@example.com", PASSWORD);
          check(s.login("new@example.com", PASSWORD).getAccountId().equals("199912345678"));
        });
    test(
        "Duplicate NIC",
        "Existing NIC",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () ->
                  s.registerCustomer(
                      "200012345678",
                      "Duplicate",
                      "0771111111",
                      "Road",
                      "unique@example.com",
                      PASSWORD));
        });
    test(
        "Duplicate email",
        "Existing email in uppercase",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () ->
                  s.registerCustomer(
                      "199912345678",
                      "Duplicate",
                      "0771111111",
                      "Road",
                      "ONE@EXAMPLE.COM",
                      PASSWORD));
        });
    test("Invalid NIC", "ABC", "Rejected", () -> rejects(() -> EUK00361749_Validation.validateNic("ABC")));
    test(
        "Legacy NIC",
        "123456789v",
        "Normalized uppercase V",
        () -> check(EUK00361749_Validation.validateNic("123456789v").equals("123456789V")));
    test(
        "Invalid contact",
        "Malformed email and phone",
        "Both rejected",
        () -> {
          rejects(() -> EUK00361749_Validation.validateEmail("bad"));
          rejects(() -> EUK00361749_Validation.validatePhone("hello"));
        });
    test(
        "Password length",
        "Short password",
        "Rejected",
        () -> rejects(() -> EUK00361749_Password.hash("short".toCharArray())));
    test(
        "Salt uniqueness",
        "Hash same password twice",
        "Different hashes; both verify",
        () -> {
          String other = EUK00361749_Password.hash(PASSWORD);
          check(!other.equals(HASH));
          check(
              EUK00361749_Password.verify(PASSWORD, HASH)
                  && EUK00361749_Password.verify(PASSWORD, other));
        });
    test(
        "Malformed hash",
        "Colon only",
        "False without crash",
        () -> check(!EUK00361749_Password.verify(PASSWORD, ":")));
    test(
        "Profile update",
        "New email and address",
        "Same NIC; new details used",
        () -> {
          EUK00361749_Service s = fixture();
          s.updateProfile("Updated", "0772222222", "New Address", "updated@example.com");
          check(s.getLoggedInAccount().getAccountId().equals("200012345678"));
          check(s.login("updated@example.com", PASSWORD).getName().equals("Updated"));
        });
    test(
        "Profile email clash",
        "Other customer email",
        "Rejected; original unchanged",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.updateProfile("Changed", "0772222222", "Road", "two@example.com"));
          check(s.getLoggedInAccount().getEmail().equals("one@example.com"));
        });
    test(
        "Password change",
        "Correct current and new password",
        "Old rejected; new works",
        () -> {
          EUK00361749_Service s = fixture();
          char[] next = "A different long password".toCharArray();
          s.changePassword(PASSWORD, next);
          rejects(() -> s.login("one@example.com", PASSWORD));
          check(s.login("one@example.com", next) instanceof EUK00361749_Customer);
        });
    test(
        "Password change denied",
        "Wrong current password",
        "Rejected; old still valid",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.changePassword("wrong".toCharArray(), PASSWORD));
          check(s.login("one@example.com", PASSWORD) != null);
        });
    test(
        "Add and update product",
        "New Z then price 3500",
        "New value returned",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.addProduct(product("Z", EUK00361749_Category.FRUITS, "3000", 8));
          s.updateProduct(product("Z", EUK00361749_Category.FRUITS, "3500", 10));
          money("3500", s.searchProducts("Z").get(0).unitPrice());
        });
    test(
        "Duplicate product",
        "Existing V1",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          rejects(() -> s.addProduct(product("V1", EUK00361749_Category.FRUITS, "3000", 8)));
        });
    test(
        "Remove unused product",
        "F1 without history",
        "Removed",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.removeProduct("F1");
          check(s.searchProducts("F1").isEmpty());
        });
    test(
        "Retain historical product",
        "V1 after order",
        "Removal rejected",
        () -> {
          EUK00361749_Service s = fixture();
          order(s);
          admin(s);
          rejects(() -> s.removeProduct("V1"));
        });
    test(
        "Product validation",
        "Negative price or stock; 3 decimals",
        "All rejected",
        () -> {
          rejects(() -> product("X", EUK00361749_Category.FRUITS, "-1", 8));
          rejects(() -> product("X", EUK00361749_Category.FRUITS, "1", -1));
          rejects(() -> product("X", EUK00361749_Category.FRUITS, "1.001", 8));
        });
    test(
        "Stock boundaries",
        "Quantities 0 1 5 6",
        "Out Low Low In",
        () -> {
          check(
              product("X", EUK00361749_Category.FRUITS, "1", 0).stockStatus()
                  == EUK00361749_StockStatus.OUT_OF_STOCK);
          check(
              product("X", EUK00361749_Category.FRUITS, "1", 1).stockStatus()
                  == EUK00361749_StockStatus.LOW_STOCK);
          check(
              product("X", EUK00361749_Category.FRUITS, "1", 5).stockStatus()
                  == EUK00361749_StockStatus.LOW_STOCK);
          check(
              product("X", EUK00361749_Category.FRUITS, "1", 6).stockStatus()
                  == EUK00361749_StockStatus.IN_STOCK);
        });
    test(
        "Category inventory",
        "DAIRY filter",
        "Only D1 returned",
        () -> {
          EUK00361749_Service s = fixture();
          check(s.viewInventory(EUK00361749_Category.DAIRY).size() == 1);
          check(s.viewInventory(EUK00361749_Category.DAIRY).get(0).productId().equals("D1"));
        });
    test(
        "Search",
        "v1 and case-insensitive Demo",
        "Exact ID one; name four",
        () -> {
          EUK00361749_Service s = fixture();
          check(s.searchProducts("v1").size() == 1);
          check(s.searchProducts("dEmO").size() == 4);
          check(s.searchProducts("absent").isEmpty());
        });
    test(
        "Empty order",
        "Empty basket",
        "Rejected; history empty",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.placeOrder(Map.of(), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
          check(s.viewOrderHistory().isEmpty());
        });
    test(
        "Bad quantity",
        "Zero and negative",
        "Both rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.placeOrder(Map.of("V1", 0), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
          rejects(() -> s.placeOrder(Map.of("V1", -1), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
        });
    test(
        "Unknown product",
        "MISSING",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () -> s.placeOrder(Map.of("MISSING", 2), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
        });
    test(
        "Duplicate normalized ID",
        "V1 and v1",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () ->
                  s.placeOrder(Map.of("V1", 2, "v1", 2), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
        });
    test(
        "Same-day delivery",
        "2026-11-10",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () ->
                  s.placeOrder(
                      Map.of("V1", 2), TOMORROW.minusDays(1), EUK00361749_TimeSlot.MORNING_09_12));
        });
    test(
        "Past delivery",
        "2026-11-09",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(
              () ->
                  s.placeOrder(
                      Map.of("V1", 2), TOMORROW.minusDays(2), EUK00361749_TimeSlot.MORNING_09_12));
        });
    test(
        "Colombo date boundary",
        "UTC 20:00 is next day in Colombo",
        "Local same-day rejected",
        () -> {
          Path p = path();
          seed().saveToFile(p);
          EUK00361749_Service s =
              new EUK00361749_Service(
                  p,
                  Clock.fixed(Instant.parse("2026-11-10T20:00:00Z"), ZoneOffset.UTC),
                  new EUK00361749_CategoryPricing());
          customer(s);
          rejects(() -> order(s));
        });
    test(
        "Tomorrow delivery",
        "Valid V1 x2",
        "Placed; stock becomes 18",
        () -> {
          EUK00361749_Service s = fixture();
          check(order(s).status() == EUK00361749_OrderStatus.PLACED);
          check(s.searchProducts("V1").get(0).stockQuantity() == 18);
        });
    test(
        "Insufficient stock",
        "V1 x21 when 20",
        "Rejected; stock remains 20",
        () -> {
          EUK00361749_Service s = fixture();
          rejects(() -> s.placeOrder(Map.of("V1", 21), TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
        });
    test(
        "Low stock excluded",
        "Stock 5 then request 2",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.updateStockQuantity("V1", 5);
          customer(s);
          rejects(() -> order(s));
        });
    test(
        "Out of stock excluded",
        "Stock 0",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.updateStockQuantity("V1", 0);
          customer(s);
          rejects(() -> order(s));
        });
    test(
        "Basket all or nothing",
        "V1 valid plus F1 x99",
        "No stock or history change",
        () -> {
          EUK00361749_Service s = fixture();
          Map<String, Integer> b = new LinkedHashMap<>();
          b.put("V1", 2);
          b.put("F1", 99);
          rejects(() -> s.placeOrder(b, TOMORROW, EUK00361749_TimeSlot.MORNING_09_12));
          check(s.searchProducts("V1").get(0).stockQuantity() == 20 && s.viewOrderHistory().isEmpty());
        });
    test(
        "Vegetable minimum",
        "1999.99 then 2000",
        "Reject below; payable 2400",
        () -> {
          rejects(() -> price(EUK00361749_Category.VEGETABLES, "1999.99"));
          money("2400", price(EUK00361749_Category.VEGETABLES, "2000").finalPayable());
        });
    test(
        "Fruit minimum",
        "2499.99 then 2500",
        "Reject below; payable 2975",
        () -> {
          rejects(() -> price(EUK00361749_Category.FRUITS, "2499.99"));
          money("2975", price(EUK00361749_Category.FRUITS, "2500").finalPayable());
        });
    test(
        "Dairy minimum",
        "2999.99 then 3000",
        "Reject below; payable 3640",
        () -> {
          rejects(() -> price(EUK00361749_Category.DAIRY, "2999.99"));
          money("3640", price(EUK00361749_Category.DAIRY, "3000").finalPayable());
        });
    test(
        "Household minimum",
        "3999.99 then 4000",
        "Reject below; payable 4900",
        () -> {
          rejects(() -> price(EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "3999.99"));
          money("4900", price(EUK00361749_Category.HOUSEHOLD_ESSENTIALS, "4000").finalPayable());
        });
    test(
        "Exact discount threshold",
        "Vegetables 10000",
        "Discount 0; total 10800",
        () -> {
          EUK00361749_Invoice i = price(EUK00361749_Category.VEGETABLES, "10000");
          money("0", i.discount());
          money("10800", i.finalPayable());
        });
    test(
        "Above discount threshold",
        "Vegetables 10000.01",
        "Discount 500; total 10275.01",
        () -> {
          EUK00361749_Invoice i = price(EUK00361749_Category.VEGETABLES, "10000.01");
          money("500", i.discount());
          money("10275.01", i.finalPayable());
        });
    test(
        "Mixed category billing",
        "Vegetables 6000; household 6000",
        "Discount 900; delivery 500; tax 825; total 12425",
        () -> {
          EUK00361749_Invoice i =
              new EUK00361749_CategoryPricing()
                  .calculate(
                      List.of(
                          new EUK00361749_OrderLine(
                              "V",
                              "Veg",
                              EUK00361749_Category.VEGETABLES,
                              new BigDecimal("6000"),
                              1),
                          new EUK00361749_OrderLine(
                              "H",
                              "Home",
                              EUK00361749_Category.HOUSEHOLD_ESSENTIALS,
                              new BigDecimal("6000"),
                              1)));
          money("900", i.discount());
          money("500", i.deliveryFee());
          money("825", i.tax());
          money("12425", i.finalPayable());
        });
    test(
        "Mixed minimum",
        "Vegetables 2000 plus household 1000",
        "Rejected below highest minimum 4000",
        () ->
            rejects(
                () ->
                    new EUK00361749_CategoryPricing()
                        .calculate(
                            List.of(
                                new EUK00361749_OrderLine(
                                    "V",
                                    "Veg",
                                    EUK00361749_Category.VEGETABLES,
                                    new BigDecimal("2000"),
                                    1),
                                new EUK00361749_OrderLine(
                                    "H",
                                    "Home",
                                    EUK00361749_Category.HOUSEHOLD_ESSENTIALS,
                                    new BigDecimal("1000"),
                                    1)))));
    test(
        "Dairy discount",
        "12000",
        "Discount 960; tax 883.20; total 12323.20",
        () -> {
          EUK00361749_Invoice i = price(EUK00361749_Category.DAIRY, "12000");
          money("960", i.discount());
          money("883.20", i.tax());
          money("12323.20", i.finalPayable());
        });
    test(
        "Rounding",
        "Vegetables 2000.10",
        "Tax 100.01 half up",
        () -> money("100.01", price(EUK00361749_Category.VEGETABLES, "2000.10").tax()));
    test(
        "Cancel placed",
        "V1 x2 then cancel",
        "Stock restored to 20; cancelled due zero",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          s.cancelOrder(o.orderId());
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
          check(s.generateInvoice(o.orderId()).contains("LKR 0.00 (cancelled)"));
        });
    test(
        "Double cancellation",
        "Cancel same order twice",
        "Second rejected; stock stays 20",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          s.cancelOrder(o.orderId());
          rejects(() -> s.cancelOrder(o.orderId()));
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
        });
    test(
        "Cancel preparing",
        "Advance once then cancel",
        "Allowed; stock restored",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          admin(s);
          s.advanceOrderStatus(o.orderId());
          customer(s);
          s.cancelOrder(o.orderId());
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
        });
    test(
        "Cancellation cutoff",
        "Advance twice then cancel",
        "Rejected at Out for Delivery",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          admin(s);
          s.advanceOrderStatus(o.orderId());
          s.advanceOrderStatus(o.orderId());
          customer(s);
          rejects(() -> s.cancelOrder(o.orderId()));
          check(s.searchProducts("V1").get(0).stockQuantity() == 18);
        });
    test(
        "Delivery terminal state",
        "Advance three times then once more",
        "Delivered; further advance rejected",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          admin(s);
          s.advanceOrderStatus(o.orderId());
          s.advanceOrderStatus(o.orderId());
          s.advanceOrderStatus(o.orderId());
          check(s.viewOrderHistory().get(0).status() == EUK00361749_OrderStatus.DELIVERED);
          rejects(() -> s.advanceOrderStatus(o.orderId()));
        });
    test(
        "Customer status control",
        "Customer attempts advance",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          rejects(() -> s.advanceOrderStatus(o.orderId()));
        });
    test(
        "Order ownership",
        "Second customer views/cancels first order",
        "History empty; invoice/cancel rejected",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          s.login("two@example.com", PASSWORD);
          check(s.viewOrderHistory().isEmpty());
          rejects(() -> s.generateInvoice(o.orderId()));
          rejects(() -> s.cancelOrder(o.orderId()));
        });
    test(
        "Historical snapshots",
        "Change profile and product price after order",
        "Original customer and price retained",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          String old = s.generateInvoice(o.orderId());
          s.updateProfile("New Name", "0771111111", "Changed", "one@example.com");
          admin(s);
          s.updateProduct(product("V1", EUK00361749_Category.VEGETABLES, "9999", 18));
          check(s.generateInvoice(o.orderId()).equals(old));
        });
    test(
        "Persistent restart",
        "Order then reopen data file",
        "Login; same order and stock",
        () -> {
          Path p = path();
          seed().saveToFile(p);
          EUK00361749_Service s =
              new EUK00361749_Service(p, CLOCK, new EUK00361749_CategoryPricing());
          customer(s);
          EUK00361749_Order o = order(s);
          EUK00361749_Service reopened =
              new EUK00361749_Service(p, CLOCK, new EUK00361749_CategoryPricing());
          customer(reopened);
          check(reopened.viewOrderHistory().get(0).orderId().equals(o.orderId()));
          check(reopened.searchProducts("V1").get(0).stockQuantity() == 18);
        });
    test(
        "No plaintext password",
        "Save credentials and inspect decoded fields",
        "Plaintext absent; PBKDF2 record present",
        () -> {
          Path p = path();
          seed().saveToFile(p);
          String decoded =
              Files.readAllLines(p).stream()
                  .skip(1)
                  .flatMap(x -> Arrays.stream(x.split("\t")))
                  .map(
                      x ->
                          new String(
                              Base64.getDecoder().decode(x),
                              java.nio.charset.StandardCharsets.UTF_8))
                  .reduce("", (a, b) -> a + "\n" + b);
          check(!decoded.contains(new String(PASSWORD)));
          check(decoded.contains(EUK00361749_Password.ITERATIONS + ":"));
        });
    test(
        "Corrupt file",
        "Invalid header",
        "Load rejected; bytes unchanged",
        () -> {
          Path p = path();
          Files.writeString(p, "damaged");
          rejects(() -> EUK00361749_Store.loadFromFile(p));
          check(Files.readString(p).equals("damaged"));
        });
    test(
        "Save failure rollback",
        "Replace database with nonempty directory",
        "Order rejected; memory stock/history unchanged",
        () -> {
          Path p = path();
          seed().saveToFile(p);
          EUK00361749_Service s =
              new EUK00361749_Service(p, CLOCK, new EUK00361749_CategoryPricing());
          customer(s);
          Files.delete(p);
          Files.createDirectory(p);
          Files.writeString(p.resolve("block"), "keep");
          rejects(() -> order(s));
          check(s.viewOrderHistory().isEmpty());
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
        });
    test(
        "Immutable snapshots",
        "Modify returned inventory and order lines",
        "Both unmodifiable",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_Order o = order(s);
          try {
            s.viewInventory(null).clear();
            throw new AssertionError();
          } catch (UnsupportedOperationException expected) {
          }
          try {
            o.lines().clear();
            throw new AssertionError();
          } catch (UnsupportedOperationException expected) {
          }
        });
    test(
        "Sequential order IDs",
        "Two orders",
        "ORD0001 then ORD0002",
        () -> {
          EUK00361749_Service s = fixture();
          String first = order(s).orderId();
          String second = order(s).orderId();
          check(first.equals("ORD0001") && second.equals("ORD0002"));
        });
    test(
        "Observer notifications",
        "Place order, admin advances it",
        "Customer receives 2 messages",
        () -> {
          EUK00361749_Service s = fixture();
          EUK00361749_CustomerNotifier notifier = new EUK00361749_CustomerNotifier();
          s.addOrderObserver(notifier);
          EUK00361749_Order o = order(s);
          admin(s);
          s.advanceOrderStatus(o.orderId());
          List<String> messages = notifier.collectMessages("200012345678");
          check(messages.size() == 2 && messages.get(1).contains("Preparing"));
          check(notifier.countMessages("200012345678") == 0);
        });
    test(
        "Order builder",
        "Builder with no lines",
        "Rejected",
        () -> {
          EUK00361749_Customer c =
              new EUK00361749_Customer(
                  "200012345678", "Builder Test", "0771234567", "Road", "b@example.com", HASH);
          rejects(
              () ->
                  new EUK00361749_Order.EUK00361749_OrderBuilder("ORD0009", c)
                      .deliverOn(TOMORROW, EUK00361749_TimeSlot.EVENING_16_20)
                      .build());
        });
    test(
        "Merge sort inventory",
        "Products added in mixed order",
        "Sorted by category then name",
        () -> {
          check(EUK00361749_Sorter.mergeSort(List.of(5, 3, 9, 1, 3), Comparator.<Integer>naturalOrder())
              .equals(List.of(1, 3, 3, 5, 9)));
          EUK00361749_Service s = fixture();
          List<EUK00361749_Product> all = s.viewInventory(null);
          check(all.get(0).category() == EUK00361749_Category.VEGETABLES);
          check(all.get(3).category() == EUK00361749_Category.HOUSEHOLD_ESSENTIALS);
        });
    test(
        "Manual stock status",
        "Admin marks V1 Out of Stock (qty 20)",
        "Order rejected; quantity unchanged",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.updateStockStatus("V1", EUK00361749_StockStatus.OUT_OF_STOCK);
          check(s.searchProducts("V1").get(0).stockQuantity() == 20);
          customer(s);
          rejects(() -> order(s));
        });
    test(
        "Contradictory status",
        "In Stock with quantity 0",
        "Rejected",
        () -> {
          EUK00361749_Service s = fixture();
          admin(s);
          s.updateStockQuantity("V1", 0);
          rejects(() -> s.updateStockStatus("V1", EUK00361749_StockStatus.IN_STOCK));
        });
    test(
        "Order quote",
        "Quote V1 x2 without placing",
        "Payable 2400; stock still 20",
        () -> {
          EUK00361749_Service s = fixture();
          money("2400", s.quoteOrder(Map.of("V1", 2)).finalPayable());
          check(s.searchProducts("V1").get(0).stockQuantity() == 20 && s.viewOrderHistory().isEmpty());
        });
    test(
        "Polymorphic account roles",
        "Admin and Customer as Account",
        "Different menu titles",
        () -> {
          List<EUK00361749_Account> accounts =
              List.of(
                  new EUK00361749_Admin("Boss", "boss@example.com", HASH),
                  new EUK00361749_Customer(
                      "200012345678", "Cust", "0771234567", "Road", "c@example.com", HASH));
          check(accounts.get(0).getMenuTitle().startsWith("ADMINISTRATOR"));
          check(accounts.get(1).getMenuTitle().startsWith("CUSTOMER"));
        });
    System.out.println("RESULT: " + passed + " passed; " + failed + " failed.");
    if (failed > 0) System.exit(1);
  }
}
