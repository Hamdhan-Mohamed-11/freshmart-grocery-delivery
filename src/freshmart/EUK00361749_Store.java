package freshmart;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory collections of accounts, products and orders, plus file saving and loading.
 *
 * <p>Data structure choice: each collection is a {@link LinkedHashMap} keyed by its unique ID, so
 * every lookup by NIC, product ID or order ID is O(1) on average while listings keep insertion
 * order. Getters return unmodifiable copies so other classes cannot change the maps directly.
 *
 * <p>The service changes data on a <em>copy</em> of the store, saves the copy, and only then
 * switches to it. If saving fails the original store is untouched (commit-before-publish).
 */
public final class EUK00361749_Store {

    private static final String FILE_HEADER = "FRESHMART-1";

    private final Map<String, EUK00361749_Account> accountsById = new LinkedHashMap<>();
    private final Map<String, EUK00361749_Product> productsById = new LinkedHashMap<>();
    private final Map<String, EUK00361749_Order> ordersById = new LinkedHashMap<>();

    /** Creates an empty store. */
    public EUK00361749_Store() {
    }

    /** Copy constructor used to prepare a change before it is saved. */
    public EUK00361749_Store(EUK00361749_Store original) {
        accountsById.putAll(original.accountsById);
        productsById.putAll(original.productsById);
        ordersById.putAll(original.ordersById);
    }

    // ---------- read access ----------

    public List<EUK00361749_Account> getAllAccounts() {
        return List.copyOf(accountsById.values());
    }

    public List<EUK00361749_Product> getAllProducts() {
        return List.copyOf(productsById.values());
    }

    public List<EUK00361749_Order> getAllOrders() {
        return List.copyOf(ordersById.values());
    }

    public EUK00361749_Account findAccount(String accountId) {
        return accountId == null ? null : accountsById.get(accountId);
    }

    public EUK00361749_Product findProduct(String productId) {
        return productsById.get(EUK00361749_Validation.validateProductId(productId));
    }

    public EUK00361749_Order findOrder(String orderId) {
        return orderId == null ? null : ordersById.get(orderId.trim().toUpperCase(java.util.Locale.ROOT));
    }

    // ---------- write access (package-private: used by the service and tests) ----------

    void saveAccount(EUK00361749_Account account) {
        accountsById.put(account.getAccountId(), account);
    }

    void saveProduct(EUK00361749_Product product) {
        productsById.put(product.productId(), product);
    }

    void saveOrder(EUK00361749_Order order) {
        ordersById.put(order.orderId().toUpperCase(java.util.Locale.ROOT), order);
    }

    void deleteProduct(String productId) {
        productsById.remove(productId);
    }

    // ---------- file persistence ----------

    /**
     * Writes every record to a text file. Fields are Base64-encoded so tabs or new lines typed by a
     * user cannot break the file format (encoding, not encryption). Data goes to a temporary file
     * that then replaces the old file, so an interrupted save never leaves half a file.
     */
    public void saveToFile(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(FILE_HEADER);
        for (EUK00361749_Account account : accountsById.values()) {
            if (account instanceof EUK00361749_Customer customer) {
                lines.add(encodeRow("C", customer.getAccountId(), customer.getName(), customer.getContactNumber(),
                        customer.getDeliveryAddress(), customer.getEmail(), customer.getPasswordHash()));
            } else {
                lines.add(encodeRow("A", account.getName(), account.getEmail(), account.getPasswordHash()));
            }
        }
        for (EUK00361749_Product product : productsById.values()) {
            lines.add(encodeRow("P", product.productId(), product.name(), product.category().name(),
                    product.unitPrice().toPlainString(), String.valueOf(product.stockQuantity()),
                    product.stockStatus().name()));
        }
        for (EUK00361749_Order order : ordersById.values()) {
            lines.add(encodeRow("O", order.orderId(), order.customerId(), order.customerDetails(),
                    order.deliveryDate().toString(), order.timeSlot().name(), order.state().getCode()));
            for (EUK00361749_OrderLine line : order.lines()) {
                lines.add(encodeRow("L", order.orderId(), line.productId(), line.name(), line.category().name(),
                        line.unitPrice().toPlainString(), String.valueOf(line.quantity())));
            }
        }

        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), "freshmart-", ".tmp");
        try {
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Reads a data file. A missing file gives an empty store (first run); a damaged file is rejected
     * and left exactly as it was.
     */
    public static EUK00361749_Store loadFromFile(Path file) throws IOException {
        EUK00361749_Store store = new EUK00361749_Store();
        if (!Files.exists(file)) {
            return store;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(FILE_HEADER)) {
            throw new IOException("Unsupported or damaged FreshMart data file.");
        }
        // Order headers and lines are collected first, then joined once every line has been read.
        Map<String, String[]> orderHeaders = new LinkedHashMap<>();
        Map<String, List<EUK00361749_OrderLine>> orderLines = new LinkedHashMap<>();
        Set<String> seenEmails = new HashSet<>();
        try {
            for (String rawLine : lines.subList(1, lines.size())) {
                String[] fields = decodeRow(rawLine);
                switch (fields[0]) {
                    case "A", "C" -> store.loadAccount(fields, seenEmails);
                    case "P" -> store.loadProduct(fields);
                    case "O" -> {
                        if (fields.length != 7 || orderHeaders.putIfAbsent(fields[1], fields) != null) {
                            throw new IllegalArgumentException("Bad order record.");
                        }
                    }
                    case "L" -> {
                        if (fields.length != 7) {
                            throw new IllegalArgumentException("Bad order line record.");
                        }
                        orderLines.computeIfAbsent(fields[1], key -> new ArrayList<>())
                                .add(new EUK00361749_OrderLine(fields[2], fields[3],
                                        EUK00361749_Category.valueOf(fields[4]), new BigDecimal(fields[5]),
                                        Integer.parseInt(fields[6])));
                    }
                    default -> throw new IllegalArgumentException("Unknown record type.");
                }
            }
            store.assembleOrders(orderHeaders, orderLines);
            return store;
        } catch (RuntimeException ex) {
            throw new IOException("Invalid stored data; the original file was not changed.", ex);
        }
    }

    private void loadAccount(String[] fields, Set<String> seenEmails) {
        EUK00361749_Account account;
        if (fields[0].equals("A") && fields.length == 4) {
            account = new EUK00361749_Admin(fields[1], fields[2], fields[3]);
        } else if (fields[0].equals("C") && fields.length == 7) {
            account = new EUK00361749_Customer(fields[1], fields[2], fields[3], fields[4], fields[5], fields[6]);
        } else {
            throw new IllegalArgumentException("Bad account record.");
        }
        if (findAccount(account.getAccountId()) != null || !seenEmails.add(account.getEmail())) {
            throw new IllegalArgumentException("Duplicate account.");
        }
        saveAccount(account);
    }

    private void loadProduct(String[] fields) {
        // 6 fields = older files without a stored status (status is then derived from quantity).
        if ((fields.length != 6 && fields.length != 7) || findProduct(fields[1]) != null) {
            throw new IllegalArgumentException("Bad product record.");
        }
        int quantity = Integer.parseInt(fields[5]);
        EUK00361749_StockStatus status = fields.length == 7
                ? EUK00361749_StockStatus.valueOf(fields[6])
                : EUK00361749_Product.statusForQuantity(quantity);
        saveProduct(new EUK00361749_Product(fields[1], fields[2], EUK00361749_Category.valueOf(fields[3]),
                new BigDecimal(fields[4]), quantity, status));
    }

    private void assembleOrders(Map<String, String[]> headers, Map<String, List<EUK00361749_OrderLine>> linesByOrder) {
        if (!linesByOrder.keySet().equals(headers.keySet())) {
            throw new IllegalArgumentException("Orders and order lines do not match.");
        }
        if (!accountsById.isEmpty() && !(findAccount(EUK00361749_Admin.ADMIN_ACCOUNT_ID) instanceof EUK00361749_Admin)) {
            throw new IllegalArgumentException("Administrator record missing.");
        }
        for (String[] header : headers.values()) {
            if (!(findAccount(header[2]) instanceof EUK00361749_Customer)) {
                throw new IllegalArgumentException("Order refers to an unknown customer.");
            }
            EUK00361749_Order order = new EUK00361749_Order(header[1], header[2], header[3], LocalDate.parse(header[4]),
                    EUK00361749_TimeSlot.valueOf(header[5]), EUK00361749_OrderState.fromCode(header[6]),
                    linesByOrder.get(header[1]));
            for (EUK00361749_OrderLine line : order.lines()) {
                if (findProduct(line.productId()) == null) {
                    throw new IllegalArgumentException("Order refers to an unknown product.");
                }
            }
            saveOrder(order);
        }
    }

    private static String encodeRow(String... fields) {
        String[] encoded = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            encoded[i] = Base64.getEncoder().encodeToString(fields[i].getBytes(StandardCharsets.UTF_8));
        }
        return String.join("\t", encoded);
    }

    private static String[] decodeRow(String line) {
        String[] parts = line.split("\t", -1);
        String[] decoded = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            decoded[i] = new String(Base64.getDecoder().decode(parts[i]), StandardCharsets.UTF_8);
        }
        return decoded;
    }
}
