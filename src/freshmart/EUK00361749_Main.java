package freshmart;

import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Terminal adapter. Business rules and authorization remain in Service. */
public final class EUK00361749_Main {
  private final EUK00361749_Service service;
  private final Console console = System.console();
  private final Scanner scanner = new Scanner(System.in);

  private EUK00361749_Main(EUK00361749_Service service) {
    this.service = service;
  }

  private String read(String prompt) {
    if (console != null) {
      String value = console.readLine("%s", prompt);
      if (value == null) throw new NoSuchElementException();
      return value.trim();
    }
    System.out.print(prompt);
    return scanner.nextLine().trim();
  }

  private char[] password(String prompt) {
    if (console != null) {
      char[] value = console.readPassword("%s", prompt);
      if (value == null) throw new NoSuchElementException();
      return value;
    }
    System.out.println("Note: password input is visible in this redirected/IDE console.");
    return read(prompt).toCharArray();
  }

  private int number(String prompt) {
    return Integer.parseInt(read(prompt));
  }

  private boolean confirm(String prompt) {
    return read(prompt + " [yes/no]: ").equalsIgnoreCase("yes");
  }

  private <T extends Enum<T>> T choice(Class<T> type) {
    T[] values = type.getEnumConstants();
    for (int i = 0; i < values.length; i++) System.out.println((i + 1) + " " + values[i]);
    int n = number("Select 1-" + values.length + ": ");
    if (n < 1 || n > values.length) throw new IllegalArgumentException("Invalid selection.");
    return values[n - 1];
  }

  private String id() {
    return EUK00361749_Validation.productId(read("Product ID: "));
  }

  private EUK00361749_Product product() {
    String id = id();
    String name = read("Product name: ");
    EUK00361749_Category category = choice(EUK00361749_Category.class);
    BigDecimal price = new BigDecimal(read("Unit price LKR: "));
    int stock = number("Stock quantity: ");
    return new EUK00361749_Product(id, name, category, price, stock);
  }

  private void list(List<?> items) {
    if (items.isEmpty()) System.out.println("No records found.");
    else items.forEach(System.out::println);
  }

  private void setup() throws IOException {
    System.out.println("FIRST RUN - create the administrator. Passwords need 15-128 characters.");
    String name = read("Administrator name: "), email = read("Email: ");
    char[] pass = password("Password: ");
    try {
      service.setupAdmin(name, email, pass);
      System.out.println("Administrator created. Log in to continue.");
    } finally {
      Arrays.fill(pass, '\0');
    }
  }

  private void register() throws IOException {
    String nic = read("NIC (9 digits + V/X or 12 digits): "),
        name = read("Name: "),
        phone = read("Phone: "),
        address = read("Delivery address: "),
        email = read("Email: ");
    char[] pass = password("Password (15-128 characters): ");
    try {
      service.register(nic, name, phone, address, email, pass);
      System.out.println("Account created. Please log in.");
    } finally {
      Arrays.fill(pass, '\0');
    }
  }

  private void login() {
    String email = read("Email: ");
    char[] pass = password("Password: ");
    try {
      EUK00361749_Account a = service.login(email, pass);
      System.out.println("Welcome " + a.name() + " | " + a.role());
    } finally {
      Arrays.fill(pass, '\0');
    }
  }

  private void browse() {
    System.out.println("0 All categories, 1 Select category");
    EUK00361749_Category c =
        read("Choice: ").equals("1") ? choice(EUK00361749_Category.class) : null;
    list(service.inventory(c));
  }

  private void place() throws IOException {
    list(service.inventory(null));
    System.out.println("Only IN_STOCK products can be ordered. Enter blank ID to finish.");
    Map<String, Integer> basket = new LinkedHashMap<>();
    while (true) {
      String raw = read("Product ID (blank ends basket): ");
      if (raw.isBlank()) break;
      String id = EUK00361749_Validation.productId(raw);
      if (basket.containsKey(id))
        throw new IllegalArgumentException("Duplicate product; restart order.");
      basket.put(id, number("Quantity: "));
    }
    LocalDate date = LocalDate.parse(read("Delivery date YYYY-MM-DD (tomorrow or later): "));
    EUK00361749_TimeSlot slot = choice(EUK00361749_TimeSlot.class);
    // Display a quote using the same pricing strategy before requesting commitment.
    List<EUK00361749_OrderLine> lines = new ArrayList<>();
    for (var e : basket.entrySet()) {
      EUK00361749_Product p =
          service.inventory(null).stream()
              .filter(x -> x.id().equals(e.getKey()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Product not found."));
      lines.add(new EUK00361749_OrderLine(p.id(), p.name(), p.category(), p.price(), e.getValue()));
    }
    System.out.println(new EUK00361749_CategoryPricing().calculate(lines));
    System.out.println("Delivery " + date + " | " + slot);
    if (confirm("Place this order")) {
      EUK00361749_Order order = service.place(basket, date, slot);
      System.out.println("Order placed successfully.");
      System.out.println(service.invoice(order.id()));
    } else System.out.println("Order abandoned; no stock changed.");
  }

  private void invoice() throws IOException {
    String id = read("Order ID: ");
    String output = service.invoice(id);
    System.out.println(output);
    if (confirm("Export invoice to invoices folder")) {
      Path directory = Path.of("invoices");
      Files.createDirectories(directory);
      // Generated IDs are UUID based; use an independent safe filename for export.
      Path target = directory.resolve("invoice-" + UUID.randomUUID() + ".txt");
      Files.writeString(target, output);
      System.out.println("Saved: " + target.toAbsolutePath());
    }
  }

  private void profile() throws IOException {
    System.out.println(service.current());
    String name = read("Name: "),
        phone = read("Phone: "),
        address = read("Address: "),
        email = read("Email: ");
    service.updateProfile(name, phone, address, email);
    System.out.println("Profile updated; NIC remains unchanged.");
  }

  private void changePassword() throws IOException {
    char[] old = password("Current password: "),
        next = password("New password (15-128 characters): ");
    try {
      service.changePassword(old, next);
      System.out.println("Password updated.");
    } finally {
      Arrays.fill(old, '\0');
      Arrays.fill(next, '\0');
    }
  }

  private void adminMenu() throws IOException {
    System.out.println(
        "\n"
            + "ADMIN | 1 Inventory | 2 Search | 3 Add product | 4 Update product | 5 Set stock | 6"
            + " Remove product | 7 Orders | 8 Advance delivery status | 9 Invoice | 0 Logout");
    switch (read("Choice: ")) {
      case "1" -> browse();
      case "2" -> list(service.search(read("Name or exact ID: ")));
      case "3" -> {
        service.addProduct(product());
        System.out.println("Product added.");
      }
      case "4" -> {
        service.updateProduct(product());
        System.out.println("Product updated.");
      }
      case "5" -> {
        service.setStock(id(), number("New stock quantity: "));
        System.out.println("Stock updated.");
      }
      case "6" -> {
        String id = id();
        if (confirm("Remove " + id)) {
          service.removeProduct(id);
          System.out.println("Product removed.");
        }
      }
      case "7" -> list(service.history());
      case "8" -> {
        String id = read("Order ID: ");
        if (confirm("Advance to the next delivery status")) {
          service.advance(id);
          System.out.println("Status advanced.");
        }
      }
      case "9" -> invoice();
      case "0" -> service.logout();
      default -> System.out.println("Invalid menu choice.");
    }
  }

  private void customerMenu() throws IOException {
    System.out.println(
        "\n"
            + "CUSTOMER | 1 Browse | 2 Search | 3 Place order | 4 Order history | 5 Cancel order |"
            + " 6 Invoice | 7 Edit profile | 8 Change password | 0 Logout");
    switch (read("Choice: ")) {
      case "1" -> browse();
      case "2" -> list(service.search(read("Name or exact ID: ")));
      case "3" -> place();
      case "4" -> list(service.history());
      case "5" -> {
        String id = read("Order ID: ");
        if (confirm("Cancel order")) {
          service.cancel(id);
          System.out.println("Order cancelled and stock restored.");
        }
      }
      case "6" -> invoice();
      case "7" -> profile();
      case "8" -> changePassword();
      case "0" -> service.logout();
      default -> System.out.println("Invalid menu choice.");
    }
  }

  private void run() {
    System.out.println("FRESHMART GROCERY DELIVERY | LKR | Sri Lanka time");
    while (true) {
      try {
        if (service.needsSetup()) {
          setup();
          continue;
        }
        EUK00361749_Account account = null;
        try {
          account = service.current();
        } catch (IllegalArgumentException ignored) {
          /* Guest menu. */
        }
        if (account == null) {
          System.out.println("\n1 Register customer | 2 Login | 0 Exit");
          switch (read("Choice: ")) {
            case "1" -> register();
            case "2" -> login();
            case "0" -> {
              return;
            }
            default -> System.out.println("Invalid menu choice.");
          }
        } else if (account instanceof EUK00361749_Admin) adminMenu();
        else customerMenu();
      } catch (NoSuchElementException e) {
        System.out.println("\nInput ended. Goodbye.");
        return;
      } catch (IllegalArgumentException | DateTimeException e) {
        System.out.println("Validation: " + e.getMessage());
      } catch (IOException e) {
        System.out.println("Storage error: " + e.getMessage() + ". Changes were not committed.");
      }
    }
  }

  public static void main(String[] args) {
    Path file = Path.of(args.length == 0 ? "data/freshmart.db" : args[0]).toAbsolutePath();
    try {
      Files.createDirectories(file.getParent());
      Path lock = file.resolveSibling(file.getFileName() + ".lock");
      try (FileChannel channel =
              FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
          FileLock held = channel.tryLock()) {
        if (held == null) {
          System.err.println("Another FreshMart process is using this data file.");
          return;
        }
        new EUK00361749_Main(
                new EUK00361749_Service(
                    file,
                    Clock.system(EUK00361749_Service.ZONE),
                    new EUK00361749_CategoryPricing()))
            .run();
      }
    } catch (IOException | OverlappingFileLockException e) {
      System.err.println(
          "Cannot start safely: " + e.getMessage() + ". Existing data was preserved.");
    }
  }
}
