package freshmart;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * All FreshMart business rules and access control, independent of the console.
 *
 * <p>Every change follows the same steps: check the logged-in role, validate the request, apply it
 * to a copy of the store, save that copy, and only then make it the live store.
 *
 * <p>Design patterns used here: orders are assembled by the
 * {@link EUK00361749_Order.EUK00361749_OrderBuilder} (Builder); status changes are delegated to the
 * order's {@link EUK00361749_OrderState} (State); the service is the <em>subject</em> that notifies
 * registered {@link EUK00361749_OrderObserver}s (Observer); bills are layered by
 * {@link EUK00361749_BillCalculator} (Decorator) using the category strategies in
 * {@link EUK00361749_PricingCatalogue} (Strategy).
 */
public final class EUK00361749_Service {

    public static final ZoneId STORE_ZONE = ZoneId.of("Asia/Colombo");
    private static final String ORDER_ID_PREFIX = "ORD";

    private final Path dataFile;
    private final Clock clock;
    private final EUK00361749_PricingCatalogue pricingCatalogue;
    private final EUK00361749_BillCalculator billCalculator;
    private final List<EUK00361749_OrderObserver> orderObservers = new ArrayList<>();
    private EUK00361749_Store store;
    private String loggedInAccountId;

    /**
     * Loads the data file and prepares the service.
     *
     * @param dataFile      file used for saving all records
     * @param clock         source of "today" (fixed in tests)
     * @param pricingCatalogue category pricing strategies used for orders, quotes and invoices
     */
    public EUK00361749_Service(Path dataFile, Clock clock, EUK00361749_PricingCatalogue pricingCatalogue)
            throws IOException {
        this.dataFile = dataFile;
        this.clock = clock.withZone(STORE_ZONE);
        this.pricingCatalogue = Objects.requireNonNull(pricingCatalogue);
        this.billCalculator = new EUK00361749_BillCalculator(pricingCatalogue);
        this.store = EUK00361749_Store.loadFromFile(dataFile);
    }

    // =====================================================================
    // Observer pattern (subject side)
    // =====================================================================

    public void addOrderObserver(EUK00361749_OrderObserver observer) {
        orderObservers.add(Objects.requireNonNull(observer));
    }

    private void notifyOrderObservers(EUK00361749_Order order, EUK00361749_OrderState previousState) {
        for (EUK00361749_OrderObserver observer : orderObservers) {
            observer.orderStatusChanged(order, previousState);
        }
    }

    // =====================================================================
    // Accounts and login
    // =====================================================================

    /** @return true when no administrator exists yet (first run). */
    public boolean needsAdministratorSetup() {
        return store.findAccount(EUK00361749_Admin.ADMIN_ACCOUNT_ID) == null;
    }

    /** Creates the one administrator account on first run. */
    public void setupAdministrator(String name, String email, char[] password) throws IOException {
        if (!needsAdministratorSetup()) {
            throw new IllegalArgumentException("The administrator account already exists.");
        }
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveAccount(new EUK00361749_Admin(name, email, EUK00361749_Password.hash(password)));
        commitChanges(updated);
    }

    /** Registers a new customer; NIC and email must both be unused. */
    public void registerCustomer(String nic, String name, String contactNumber, String deliveryAddress,
                                 String email, char[] password) throws IOException {
        if (needsAdministratorSetup()) {
            throw new IllegalArgumentException("The administrator must be set up first.");
        }
        if (store.findAccount(EUK00361749_Validation.validateNic(nic)) != null) {
            throw new IllegalArgumentException("This NIC is already registered.");
        }
        ensureEmailNotTaken(email, null);
        EUK00361749_Customer customer = new EUK00361749_Customer(nic, name, contactNumber, deliveryAddress, email,
                EUK00361749_Password.hash(password));
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveAccount(customer);
        commitChanges(updated);
    }

    /**
     * Logs in by email and password. The previous session is always ended first, so a failed
     * attempt never leaves someone else logged in.
     */
    public EUK00361749_Account login(String email, char[] password) {
        logout();
        String normalisedEmail = EUK00361749_Validation.validateEmail(email);
        for (EUK00361749_Account account : store.getAllAccounts()) {
            if (account.getEmail().equals(normalisedEmail) && account.checkPassword(password)) {
                loggedInAccountId = account.getAccountId();
                return account;
            }
        }
        throw new IllegalArgumentException("Incorrect email or password.");
    }

    public void logout() {
        loggedInAccountId = null;
    }

    /** @return the logged-in account, or null if nobody is logged in. */
    public EUK00361749_Account findLoggedInAccount() {
        return store.findAccount(loggedInAccountId);
    }

    /** @return the logged-in account; fails if nobody is logged in. */
    public EUK00361749_Account getLoggedInAccount() {
        EUK00361749_Account account = findLoggedInAccount();
        if (account == null) {
            throw new IllegalArgumentException("Please log in first.");
        }
        return account;
    }

    /** Updates the logged-in customer's details. The NIC cannot be changed. */
    public void updateProfile(String name, String contactNumber, String deliveryAddress, String email) throws IOException {
        EUK00361749_Customer customer = requireCustomer();
        ensureEmailNotTaken(email, customer.getAccountId());
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveAccount(new EUK00361749_Customer(customer.getAccountId(), name, contactNumber, deliveryAddress,
                email, customer.getPasswordHash()));
        commitChanges(updated);
    }

    /** Changes the logged-in customer's password after checking the current one. */
    public void changePassword(char[] currentPassword, char[] newPassword) throws IOException {
        EUK00361749_Customer customer = requireCustomer();
        if (!customer.checkPassword(currentPassword)) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveAccount(new EUK00361749_Customer(customer.getAccountId(), customer.getName(),
                customer.getContactNumber(), customer.getDeliveryAddress(), customer.getEmail(),
                EUK00361749_Password.hash(newPassword)));
        commitChanges(updated);
    }

    // =====================================================================
    // Administrator: products and stock
    // =====================================================================

    public void addProduct(EUK00361749_Product product) throws IOException {
        requireAdministrator();
        if (store.findProduct(product.productId()) != null) {
            throw new IllegalArgumentException("Product " + product.productId() + " already exists.");
        }
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveProduct(product);
        commitChanges(updated);
    }

    public void updateProduct(EUK00361749_Product product) throws IOException {
        requireAdministrator();
        findProductOrFail(product.productId());
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveProduct(product);
        commitChanges(updated);
    }

    /** Sets a new stock quantity; the stock status is recalculated automatically. */
    public void updateStockQuantity(String productId, int newQuantity) throws IOException {
        requireAdministrator();
        updateProduct(findProductOrFail(productId).withStockQuantity(newQuantity));
    }

    /** Sets the stock status directly (e.g. mark a product Out of Stock while checking it). */
    public void updateStockStatus(String productId, EUK00361749_StockStatus newStatus) throws IOException {
        requireAdministrator();
        updateProduct(findProductOrFail(productId).withStockStatus(newStatus));
    }

    /** Removes a product unless an order refers to it (old invoices must stay valid). */
    public void removeProduct(String productId) throws IOException {
        requireAdministrator();
        EUK00361749_Product product = findProductOrFail(productId);
        for (EUK00361749_Order order : store.getAllOrders()) {
            for (EUK00361749_OrderLine line : order.lines()) {
                if (line.productId().equals(product.productId())) {
                    throw new IllegalArgumentException("Product has order history; set its stock to 0 instead.");
                }
            }
        }
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.deleteProduct(product.productId());
        commitChanges(updated);
    }

    // =====================================================================
    // Browsing and searching (any logged-in user)
    // =====================================================================

    /**
     * Lists products in one category, or all products when {@code category} is null, sorted by
     * category and then name using the hand-written merge sort.
     */
    public List<EUK00361749_Product> viewInventory(EUK00361749_Category category) {
        getLoggedInAccount();
        List<EUK00361749_Product> selected = new ArrayList<>();
        for (EUK00361749_Product product : store.getAllProducts()) {
            if (category == null || product.category() == category) {
                selected.add(product);
            }
        }
        Comparator<EUK00361749_Product> byCategoryThenName = Comparator
                .comparing(EUK00361749_Product::category)
                .thenComparing(product -> product.name().toLowerCase(Locale.ROOT));
        return List.copyOf(EUK00361749_Sorter.mergeSort(selected, byCategoryThenName));
    }

    /**
     * Linear search: a product matches if its ID equals the query or its name contains the query
     * (both case-insensitive).
     */
    public List<EUK00361749_Product> searchProducts(String nameOrId) {
        getLoggedInAccount();
        String query = EUK00361749_Validation.requireText(nameOrId, "Search text", 100).toLowerCase(Locale.ROOT);
        List<EUK00361749_Product> matches = new ArrayList<>();
        for (EUK00361749_Product product : store.getAllProducts()) {
            if (product.productId().equalsIgnoreCase(query) || product.name().toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(product);
            }
        }
        return List.copyOf(matches);
    }

    /** @return true if a product with this ID exists (lets the console warn the administrator early). */
    public boolean productExists(String productId) {
        requireAdministrator();
        return store.findProduct(productId) != null;
    }

    // =====================================================================
    // Customer: orders
    // =====================================================================

    /**
     * Looks up a product while the customer fills the basket, so a product that cannot be ordered
     * is reported at once. The order builder checks the same rule again before the order is made.
     */
    public EUK00361749_Product findOrderableProduct(String productId) {
        requireCustomer();
        EUK00361749_Product product = findProductOrFail(productId);
        if (product.stockStatus() != EUK00361749_StockStatus.IN_STOCK) {
            throw new IllegalArgumentException(product.productId() + " is " + product.stockStatus()
                    + "; only In Stock products can be ordered.");
        }
        return product;
    }

    /**
     * Prices a basket without placing the order, so the customer can review it first. The same
     * builder validation runs as for a real order, so the preview never shows an order that would
     * later be refused.
     */
    public EUK00361749_Invoice quoteOrder(Map<String, Integer> basket, LocalDate deliveryDate,
                                          EUK00361749_TimeSlot timeSlot) {
        List<EUK00361749_OrderLine> lines = prepareOrder(basket, deliveryDate, timeSlot).validate();
        return billCalculator.calculate(lines);
    }

    /**
     * Places an order (the main algorithm): the builder validates every order rule, then stock is
     * reserved, the order is saved with status Placed and observers are notified.
     *
     * @param basket       product ID mapped to quantity
     * @param deliveryDate tomorrow or later (same-day delivery is not allowed)
     * @param timeSlot     delivery window
     */
    public EUK00361749_Order placeOrder(Map<String, Integer> basket, LocalDate deliveryDate,
                                        EUK00361749_TimeSlot timeSlot) throws IOException {
        EUK00361749_Order order = prepareOrder(basket, deliveryDate, timeSlot).build(generateNextOrderId());
        EUK00361749_Store updated = new EUK00361749_Store(store);
        for (EUK00361749_OrderLine line : order.lines()) {
            EUK00361749_Product product = findProductOrFail(line.productId());
            updated.saveProduct(product.withStockQuantity(product.stockQuantity() - line.quantity()));
        }
        updated.saveOrder(order);
        commitChanges(updated);
        notifyOrderObservers(order, null);
        return order;
    }

    /**
     * Order history sorted by delivery date: a customer sees only their own orders; the
     * administrator sees every order.
     */
    public List<EUK00361749_Order> viewOrderHistory() {
        EUK00361749_Account account = getLoggedInAccount();
        boolean isAdministrator = account instanceof EUK00361749_Admin;
        List<EUK00361749_Order> visible = new ArrayList<>();
        for (EUK00361749_Order order : store.getAllOrders()) {
            if (isAdministrator || order.customerId().equals(account.getAccountId())) {
                visible.add(order);
            }
        }
        Comparator<EUK00361749_Order> byDeliveryDate = Comparator
                .comparing(EUK00361749_Order::deliveryDate)
                .thenComparing(EUK00361749_Order::orderId);
        return List.copyOf(EUK00361749_Sorter.mergeSort(visible, byDeliveryDate));
    }

    /**
     * Cancels an order and returns its items to stock. The order's current state decides whether
     * cancellation is allowed (only Placed and Preparing allow it).
     */
    public void cancelOrder(String orderId) throws IOException {
        requireCustomer();
        EUK00361749_Order order = findAccessibleOrder(orderId);
        EUK00361749_Order cancelled = order.cancel(); // State pattern: refused from Out for Delivery on
        EUK00361749_Store updated = new EUK00361749_Store(store);
        for (EUK00361749_OrderLine line : order.lines()) {
            EUK00361749_Product product = findProductOrFail(line.productId());
            updated.saveProduct(product.withStockQuantity(Math.addExact(product.stockQuantity(), line.quantity())));
        }
        updated.saveOrder(cancelled);
        commitChanges(updated);
        notifyOrderObservers(cancelled, order.state());
    }

    /** Administrator moves an order to its next delivery stage; the current state chooses it. */
    public EUK00361749_Order advanceOrderStatus(String orderId) throws IOException {
        requireAdministrator();
        EUK00361749_Order order = findAccessibleOrder(orderId);
        EUK00361749_Order advanced = order.advance(); // State pattern
        EUK00361749_Store updated = new EUK00361749_Store(store);
        updated.saveOrder(advanced);
        commitChanges(updated);
        notifyOrderObservers(advanced, order.state());
        return advanced;
    }

    /** @return the formatted invoice for an order the logged-in user may see. */
    public String generateInvoice(String orderId) {
        EUK00361749_Order order = findAccessibleOrder(orderId);
        return billCalculator.calculate(order.lines()).toDocument(order);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Saves the prepared copy first; only a successful save replaces the live store. */
    private void commitChanges(EUK00361749_Store updated) throws IOException {
        updated.saveToFile(dataFile);
        store = updated;
    }

    /**
     * Feeds a basket into a new order builder (Builder pattern). The builder applies the order
     * rules; product IDs are matched case-insensitively by the store.
     */
    private EUK00361749_Order.EUK00361749_OrderBuilder prepareOrder(Map<String, Integer> basket,
                                                                     LocalDate deliveryDate,
                                                                     EUK00361749_TimeSlot timeSlot) {
        EUK00361749_Customer customer = requireCustomer();
        EUK00361749_Order.EUK00361749_OrderBuilder builder =
                new EUK00361749_Order.EUK00361749_OrderBuilder(pricingCatalogue, LocalDate.now(clock))
                        .forCustomer(customer)
                        .deliverOn(deliveryDate)
                        .inTimeSlot(timeSlot);
        for (Map.Entry<String, Integer> entry : basket.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            builder.addProduct(findProductOrFail(entry.getKey()), entry.getValue());
        }
        return builder;
    }

    private void requireAdministrator() {
        if (!(getLoggedInAccount() instanceof EUK00361749_Admin)) {
            throw new IllegalArgumentException("Only the administrator can do this.");
        }
    }

    private EUK00361749_Customer requireCustomer() {
        if (!(getLoggedInAccount() instanceof EUK00361749_Customer customer)) {
            throw new IllegalArgumentException("Only customers can do this.");
        }
        return customer;
    }

    private void ensureEmailNotTaken(String email, String ownAccountId) {
        String normalisedEmail = EUK00361749_Validation.validateEmail(email);
        for (EUK00361749_Account account : store.getAllAccounts()) {
            if (!account.getAccountId().equals(ownAccountId) && account.getEmail().equals(normalisedEmail)) {
                throw new IllegalArgumentException("This email is already registered.");
            }
        }
    }

    private EUK00361749_Product findProductOrFail(String productId) {
        EUK00361749_Product product = store.findProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product " + productId.trim().toUpperCase(Locale.ROOT) + " not found.");
        }
        return product;
    }

    /** Customers may only open their own orders; the administrator may open any order. */
    private EUK00361749_Order findAccessibleOrder(String orderId) {
        EUK00361749_Account account = getLoggedInAccount();
        EUK00361749_Order order = store.findOrder(orderId);
        boolean allowed = order != null
                && (account instanceof EUK00361749_Admin || order.customerId().equals(account.getAccountId()));
        if (!allowed) {
            throw new IllegalArgumentException("Order not found.");
        }
        return order;
    }

    /** Next ID in the sequence ORD0001, ORD0002, ... */
    private String generateNextOrderId() {
        int highestNumber = 0;
        for (EUK00361749_Order order : store.getAllOrders()) {
            if (order.orderId().matches(ORDER_ID_PREFIX + "\\d+")) {
                highestNumber = Math.max(highestNumber, Integer.parseInt(order.orderId().substring(ORDER_ID_PREFIX.length())));
            }
        }
        return String.format("%s%04d", ORDER_ID_PREFIX, highestNumber + 1);
    }
}
