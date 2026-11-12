package freshmart;

import java.io.Console;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command-line user interface and program entry point.
 *
 * <p>This class only reads input and prints output; every rule is enforced by
 * {@link EUK00361749_Service}. Usability features: numbered vertical menus, inputs re-asked when
 * invalid, a price preview before an order is placed, short order IDs (ORD0001), hidden password
 * entry in a real terminal, and order-status notifications (Observer pattern).
 */
public final class EUK00361749_Main {

    private static final String DIVIDER = "-".repeat(70);

    private final EUK00361749_Service service;
    private final EUK00361749_CustomerNotifier notifier = new EUK00361749_CustomerNotifier();
    private final Console console = System.console();
    private final Scanner scanner = new Scanner(System.in);
    private final Path invoiceFolder;

    private EUK00361749_Main(EUK00361749_Service service, Path invoiceFolder) {
        this.service = service;
        this.invoiceFolder = invoiceFolder;
        service.addOrderObserver(notifier);
    }

    // =====================================================================
    // Main loop and menus
    // =====================================================================

    private void run() {
        printHeading("FRESHMART GROCERY DELIVERY SYSTEM");
        while (true) {
            try {
                if (service.needsAdministratorSetup()) {
                    setupAdministrator();
                    continue;
                }
                EUK00361749_Account account = service.findLoggedInAccount();
                if (account == null) {
                    if (!startMenu()) {
                        System.out.println("Goodbye.");
                        return;
                    }
                } else if (account instanceof EUK00361749_Admin) {
                    administratorMenu(account);
                } else {
                    customerMenu(account);
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Storage error: " + ex.getMessage() + " The change was not saved.");
            }
        }
    }

    /** @return false when the user chooses Exit. */
    private boolean startMenu() throws IOException {
        System.out.println("\nWELCOME");
        System.out.println("  1. Register as a new customer");
        System.out.println("  2. Log in");
        System.out.println("  0. Exit");
        switch (readInt("Choose an option", 0, 2)) {
            case 1 -> registerCustomer();
            case 2 -> login();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void administratorMenu(EUK00361749_Account administrator) throws IOException {
        printHeading(administrator.getMenuTitle());
        System.out.println("  1. View inventory by category");
        System.out.println("  2. Search products (name or ID)");
        System.out.println("  3. Add product");
        System.out.println("  4. Update product details");
        System.out.println("  5. Update stock quantity");
        System.out.println("  6. Update stock status");
        System.out.println("  7. Remove product");
        System.out.println("  8. View all orders");
        System.out.println("  9. Advance an order's delivery status");
        System.out.println(" 10. View invoice");
        System.out.println("  0. Log out");
        switch (readInt("Choose an option", 0, 10)) {
            case 1 -> browseInventory();
            case 2 -> printProducts(service.searchProducts(readText("Product name or ID")));
            case 3 -> {
                service.addProduct(readProductDetails());
                System.out.println("Product added.");
            }
            case 4 -> {
                service.updateProduct(readProductDetails());
                System.out.println("Product updated.");
            }
            case 5 -> {
                String productId = readText("Product ID");
                service.updateStockQuantity(productId, readInt("New stock quantity", 0, 1_000_000));
                System.out.println("Stock quantity updated.");
            }
            case 6 -> {
                String productId = readText("Product ID");
                service.updateStockStatus(productId, chooseOption("New stock status", EUK00361749_StockStatus.values()));
                System.out.println("Stock status updated.");
            }
            case 7 -> removeProduct();
            case 8 -> printOrders(service.viewOrderHistory());
            case 9 -> {
                String orderId = readText("Order ID");
                if (confirm("Move order " + orderId.toUpperCase() + " to its next delivery status?")) {
                    EUK00361749_Order order = service.advanceOrderStatus(orderId);
                    System.out.println("Order " + order.orderId() + " is now " + order.status() + ".");
                }
            }
            case 10 -> viewInvoice();
            default -> logout();
        }
    }

    private void customerMenu(EUK00361749_Account customer) throws IOException {
        printHeading(customer.getMenuTitle());
        int unread = notifier.countMessages(customer.getAccountId());
        if (unread > 0) {
            System.out.println("  You have " + unread + " new notification(s) - choose option 9.");
        }
        System.out.println("  1. Browse products by category");
        System.out.println("  2. Search products (name or ID)");
        System.out.println("  3. Place a delivery order");
        System.out.println("  4. View order history");
        System.out.println("  5. Cancel an order");
        System.out.println("  6. View / export invoice");
        System.out.println("  7. Edit my profile");
        System.out.println("  8. Change password");
        System.out.println("  9. View notifications");
        System.out.println("  0. Log out");
        switch (readInt("Choose an option", 0, 9)) {
            case 1 -> browseInventory();
            case 2 -> printProducts(service.searchProducts(readText("Product name or ID")));
            case 3 -> placeOrder();
            case 4 -> printOrders(service.viewOrderHistory());
            case 5 -> cancelOrder();
            case 6 -> viewInvoice();
            case 7 -> editProfile();
            case 8 -> changePassword();
            case 9 -> showNotifications(customer);
            default -> logout();
        }
    }

    // =====================================================================
    // Account actions
    // =====================================================================

    private void setupAdministrator() throws IOException {
        printHeading("FIRST RUN - CREATE THE ADMINISTRATOR ACCOUNT");
        String name = readText("Administrator name");
        String email = readText("Administrator email");
        char[] password = readPassword("Password (at least " + EUK00361749_Password.MINIMUM_LENGTH + " characters)");
        try {
            service.setupAdministrator(name, email, password);
            System.out.println("Administrator created. Please log in.");
        } finally {
            Arrays.fill(password, '\0'); // clear the password from memory
        }
    }

    private void registerCustomer() throws IOException {
        String nic = readText("NIC (9 digits + V/X, or 12 digits)");
        String name = readText("Full name");
        String contactNumber = readText("Contact number");
        String deliveryAddress = readText("Delivery address");
        String email = readText("Email address");
        char[] password = readPassword("Password (at least " + EUK00361749_Password.MINIMUM_LENGTH + " characters)");
        try {
            service.registerCustomer(nic, name, contactNumber, deliveryAddress, email, password);
            System.out.println("Account created. You can now log in with " + email.trim().toLowerCase() + ".");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void login() {
        String email = readText("Email");
        char[] password = readPassword("Password");
        try {
            EUK00361749_Account account = service.login(email, password);
            System.out.println("Welcome, " + account.getName() + ".");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void logout() {
        service.logout();
        System.out.println("You have been logged out.");
    }

    private void editProfile() throws IOException {
        System.out.println("Current details: " + service.getLoggedInAccount());
        service.updateProfile(readText("Full name"), readText("Contact number"),
                readText("Delivery address"), readText("Email address"));
        System.out.println("Profile updated (your NIC cannot be changed).");
    }

    private void changePassword() throws IOException {
        char[] currentPassword = readPassword("Current password");
        char[] newPassword = readPassword("New password (at least " + EUK00361749_Password.MINIMUM_LENGTH + " characters)");
        try {
            service.changePassword(currentPassword, newPassword);
            System.out.println("Password changed.");
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    private void showNotifications(EUK00361749_Account customer) {
        List<String> messages = notifier.collectMessages(customer.getAccountId());
        if (messages.isEmpty()) {
            System.out.println("No new notifications.");
        } else {
            messages.forEach(message -> System.out.println("  * " + message));
        }
    }

    // =====================================================================
    // Product and order actions
    // =====================================================================

    private void browseInventory() {
        System.out.println("  0. All categories");
        EUK00361749_Category[] categories = EUK00361749_Category.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.println("  " + (i + 1) + ". " + categories[i]);
        }
        int choice = readInt("Category", 0, categories.length);
        printProducts(service.viewInventory(choice == 0 ? null : categories[choice - 1]));
    }

    private EUK00361749_Product readProductDetails() {
        String productId = readText("Product ID (e.g. V001)");
        String name = readText("Product name");
        EUK00361749_Category category = chooseOption("Category", EUK00361749_Category.values());
        BigDecimal unitPrice = readMoney("Unit price (LKR)");
        int quantity = readInt("Stock quantity", 0, 1_000_000);
        return new EUK00361749_Product(productId, name, category, unitPrice, quantity);
    }

    private void removeProduct() throws IOException {
        String productId = readText("Product ID");
        if (confirm("Permanently remove product " + productId.toUpperCase() + "?")) {
            service.removeProduct(productId);
            System.out.println("Product removed.");
        }
    }

    /** Builds a basket, shows the full price, then places the order only after confirmation. */
    private void placeOrder() throws IOException {
        printProducts(service.viewInventory(null));
        System.out.println("Only products marked In Stock can be ordered. Press Enter on an empty line to finish.");
        Map<String, Integer> basket = new LinkedHashMap<>();
        while (true) {
            String productId = readText("Product ID", true);
            if (productId.isEmpty()) {
                break;
            }
            String normalisedId = productId.toUpperCase();
            if (basket.containsKey(normalisedId)) {
                System.out.println("  " + normalisedId + " is already in the basket.");
                continue;
            }
            basket.put(normalisedId, readInt("Quantity", 1, 1000));
        }
        if (basket.isEmpty()) {
            System.out.println("The basket is empty; no order was placed.");
            return;
        }
        LocalDate deliveryDate = readDate("Delivery date (tomorrow or later)");
        EUK00361749_TimeSlot timeSlot = chooseOption("Delivery time slot", EUK00361749_TimeSlot.values());

        System.out.println("\nORDER PREVIEW - delivery " + deliveryDate + ", " + timeSlot);
        System.out.println(service.quoteOrder(basket));
        if (!confirm("Place this order?")) {
            System.out.println("Order not placed; stock is unchanged.");
            return;
        }
        EUK00361749_Order order = service.placeOrder(basket, deliveryDate, timeSlot);
        System.out.println("Order placed. Your order ID is " + order.orderId() + ".");
    }

    private void cancelOrder() throws IOException {
        String orderId = readText("Order ID");
        if (confirm("Cancel order " + orderId.toUpperCase() + "?")) {
            service.cancelOrder(orderId);
            System.out.println("Order cancelled and items returned to stock.");
        }
    }

    private void viewInvoice() throws IOException {
        String orderId = readText("Order ID").toUpperCase();
        String invoice = service.generateInvoice(orderId);
        System.out.println(invoice);
        if (confirm("Save this invoice as a text file?")) {
            Files.createDirectories(invoiceFolder);
            Path file = invoiceFolder.resolve("invoice-" + orderId + ".txt");
            Files.writeString(file, invoice);
            System.out.println("Saved to " + file.toAbsolutePath());
        }
    }

    // =====================================================================
    // Output helpers
    // =====================================================================

    private static void printHeading(String title) {
        System.out.println("\n" + DIVIDER);
        System.out.println("  " + title);
        System.out.println(DIVIDER);
    }

    private static void printProducts(List<EUK00361749_Product> products) {
        if (products.isEmpty()) {
            System.out.println("No products found.");
            return;
        }
        System.out.printf("%-8s | %-24s | %-20s | %14s | %8s | %s%n",
                "ID", "Name", "Category", "Unit price", "Quantity", "Status");
        products.forEach(System.out::println);
    }

    private static void printOrders(List<EUK00361749_Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }
        orders.forEach(System.out::println);
    }

    // =====================================================================
    // Input helpers (re-ask until valid)
    // =====================================================================

    private String readText(String prompt) {
        return readText(prompt, false);
    }

    private String readText(String prompt, boolean allowBlank) {
        while (true) {
            String value;
            if (console != null) {
                value = console.readLine("%s: ", prompt);
                if (value == null) {
                    throw new NoSuchElementException();
                }
            } else {
                System.out.print(prompt + ": ");
                value = scanner.nextLine();
            }
            value = value.trim();
            if (!value.isEmpty() || allowBlank) {
                return value;
            }
            System.out.println("  This field cannot be empty.");
        }
    }

    /** Reads a password without echo in a real terminal (the IDE console cannot hide input). */
    private char[] readPassword(String prompt) {
        if (console != null) {
            char[] password = console.readPassword("%s: ", prompt);
            if (password == null) {
                throw new NoSuchElementException();
            }
            return password;
        }
        return readText(prompt + " (visible in this console)").toCharArray();
    }

    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            String value = readText(prompt + " (" + minimum + "-" + maximum + ")");
            try {
                int number = Integer.parseInt(value);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // handled by the message below
            }
            System.out.println("  Please enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    private BigDecimal readMoney(String prompt) {
        while (true) {
            try {
                BigDecimal amount = new BigDecimal(readText(prompt));
                if (amount.signum() > 0 && amount.scale() <= 2) {
                    return amount;
                }
            } catch (NumberFormatException ignored) {
                // handled by the message below
            }
            System.out.println("  Enter a positive amount with up to 2 decimals, e.g. 450 or 450.50.");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readText(prompt + " (YYYY-MM-DD)"));
            } catch (DateTimeParseException ex) {
                System.out.println("  Dates must use the format YYYY-MM-DD, e.g. 2026-12-24.");
            }
        }
    }

    private <T> T chooseOption(String prompt, T[] options) {
        System.out.println(prompt + ":");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        return options[readInt("Select", 1, options.length) - 1];
    }

    private boolean confirm(String question) {
        while (true) {
            String answer = readText(question + " (Y/N)");
            if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")) {
                return true;
            }
            if (answer.equalsIgnoreCase("N") || answer.equalsIgnoreCase("NO")) {
                return false;
            }
            System.out.println("  Please answer Y or N.");
        }
    }

    // =====================================================================
    // Entry point
    // =====================================================================

    /**
     * Starts FreshMart. An optional argument sets the data file (default data/freshmart.db). A file
     * lock stops two copies of the program writing to the same file at once.
     */
    public static void main(String[] args) {
        Path dataFile = Path.of(args.length == 0 ? "data/freshmart.db" : args[0]).toAbsolutePath();
        try {
            Files.createDirectories(dataFile.getParent());
            Path lockFile = dataFile.resolveSibling(dataFile.getFileName() + ".lock");
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    System.err.println("FreshMart is already running with this data file.");
                    return;
                }
                EUK00361749_Service service = new EUK00361749_Service(dataFile,
                        Clock.system(EUK00361749_Service.STORE_ZONE), new EUK00361749_CategoryPricing());
                new EUK00361749_Main(service, dataFile.getParent().resolve("invoices")).run();
            }
        } catch (NoSuchElementException ex) {
            System.out.println("\nInput ended. All saved records are kept.");
        } catch (IOException | OverlappingFileLockException ex) {
            System.err.println("FreshMart could not start safely: " + ex.getMessage() + " Existing data was not changed.");
        }
    }
}
