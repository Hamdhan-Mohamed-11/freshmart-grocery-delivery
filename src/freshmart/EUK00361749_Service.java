package freshmart;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

/** Authenticated use cases. Each mutation is validated, saved, then published. */
public final class EUK00361749_Service {
  public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
  private EUK00361749_Store store;
  private final Path file;
  private final Clock clock;
  private final EUK00361749_PricingPolicy pricing;
  private String sessionId;

  public EUK00361749_Service(Path file, Clock clock, EUK00361749_PricingPolicy pricing)
      throws IOException {
    this.file = file;
    this.clock = clock.withZone(ZONE);
    this.pricing = Objects.requireNonNull(pricing);
    store = EUK00361749_Store.load(file);
  }

  private void commit(EUK00361749_Store candidate) throws IOException {
    candidate.save(file);
    store = candidate;
  }

  public boolean needsSetup() {
    return store.account("ADMIN") == null;
  }

  public void setupAdmin(String name, String email, char[] password) throws IOException {
    if (!needsSetup()) throw new IllegalArgumentException("Administrator already configured.");
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(new EUK00361749_Admin(name, email, EUK00361749_Password.hash(password)));
    commit(next);
  }

  private void uniqueEmail(String email, String excluded) {
    String normal = EUK00361749_Validation.email(email);
    if (store.accounts().stream()
        .anyMatch(a -> !a.id().equals(excluded) && a.email().equals(normal)))
      throw new IllegalArgumentException("Email already registered.");
  }

  public void register(
      String nic, String name, String phone, String address, String email, char[] password)
      throws IOException {
    if (needsSetup()) throw new IllegalArgumentException("Configure administrator first.");
    if (store.account(EUK00361749_Validation.nic(nic)) != null)
      throw new IllegalArgumentException("NIC already registered.");
    uniqueEmail(email, "");
    EUK00361749_Customer c =
        new EUK00361749_Customer(
            nic, name, phone, address, email, EUK00361749_Password.hash(password));
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(c);
    commit(next);
  }

  public EUK00361749_Account login(String email, char[] password) {
    logout();
    String normal = EUK00361749_Validation.email(email);
    EUK00361749_Account a =
        store.accounts().stream().filter(x -> x.email().equals(normal)).findFirst().orElse(null);
    if (a == null || !a.authenticates(password))
      throw new IllegalArgumentException("Invalid email or password.");
    sessionId = a.id();
    return a;
  }

  public void logout() {
    sessionId = null;
  }

  public EUK00361749_Account current() {
    EUK00361749_Account a = store.account(sessionId);
    if (a == null) throw new IllegalArgumentException("Login required.");
    return a;
  }

  private void admin() {
    if (!(current() instanceof EUK00361749_Admin))
      throw new IllegalArgumentException("Administrator access required.");
  }

  private EUK00361749_Customer customer() {
    if (!(current() instanceof EUK00361749_Customer c))
      throw new IllegalArgumentException("Customer access required.");
    return c;
  }

  public void updateProfile(String name, String phone, String address, String email)
      throws IOException {
    EUK00361749_Customer c = customer();
    uniqueEmail(email, c.id());
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(new EUK00361749_Customer(c.id(), name, phone, address, email, c.passwordHash()));
    commit(next);
  }

  public void changePassword(char[] oldPassword, char[] newPassword) throws IOException {
    EUK00361749_Customer c = customer();
    if (!c.authenticates(oldPassword))
      throw new IllegalArgumentException("Current password is incorrect.");
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(
        new EUK00361749_Customer(
            c.id(),
            c.name(),
            c.phone(),
            c.address(),
            c.email(),
            EUK00361749_Password.hash(newPassword)));
    commit(next);
  }

  public void addProduct(EUK00361749_Product product) throws IOException {
    admin();
    if (store.product(product.id()) != null)
      throw new IllegalArgumentException("Product ID exists.");
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(product);
    commit(next);
  }

  private EUK00361749_Product product(String id) {
    EUK00361749_Product p = store.product(id);
    if (p == null) throw new IllegalArgumentException("Product not found.");
    return p;
  }

  public void updateProduct(EUK00361749_Product product) throws IOException {
    admin();
    product(product.id());
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(product);
    commit(next);
  }

  public void setStock(String id, int quantity) throws IOException {
    admin();
    updateProduct(product(id).withQuantity(quantity));
  }

  public void removeProduct(String id) throws IOException {
    admin();
    EUK00361749_Product p = product(id);
    if (store.orders().stream()
        .flatMap(o -> o.lines().stream())
        .anyMatch(l -> l.productId().equals(p.id())))
      throw new IllegalArgumentException(
          "Product has order history; retain it with zero stock instead.");
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.removeProduct(p.id());
    commit(next);
  }

  public List<EUK00361749_Product> inventory(EUK00361749_Category category) {
    current();
    return store.products().stream()
        .filter(p -> category == null || p.category() == category)
        .toList();
  }

  public List<EUK00361749_Product> search(String query) {
    current();
    String q = EUK00361749_Validation.text(query, "Search", 100).toLowerCase(Locale.ROOT);
    return store.products().stream()
        .filter(p -> p.id().equalsIgnoreCase(q) || p.name().toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public EUK00361749_Order place(
      Map<String, Integer> basket, LocalDate date, EUK00361749_TimeSlot slot) throws IOException {
    EUK00361749_Customer c = customer();
    if (date == null || !date.isAfter(LocalDate.now(clock)))
      throw new IllegalArgumentException("Delivery must be tomorrow or later.");
    Objects.requireNonNull(slot);
    Map<String, Integer> normal = new LinkedHashMap<>();
    for (var e : basket.entrySet()) {
      if (e.getValue() == null || e.getValue() <= 0)
        throw new IllegalArgumentException("Quantity must be positive.");
      String id = EUK00361749_Validation.productId(e.getKey());
      if (normal.putIfAbsent(id, e.getValue()) != null)
        throw new IllegalArgumentException("Duplicate product in basket.");
    }
    List<EUK00361749_OrderLine> lines = new ArrayList<>();
    for (var e : normal.entrySet()) {
      EUK00361749_Product p = product(e.getKey());
      if (p.stockStatus() != EUK00361749_StockStatus.IN_STOCK)
        throw new IllegalArgumentException(p.id() + " is not In Stock.");
      if (e.getValue() > p.quantity())
        throw new IllegalArgumentException("Insufficient quantity for " + p.id());
      lines.add(new EUK00361749_OrderLine(p.id(), p.name(), p.category(), p.price(), e.getValue()));
    }
    pricing.calculate(lines); // Validates minimum before any stock mutation.
    EUK00361749_Order order =
        new EUK00361749_Order(
            "OR-" + UUID.randomUUID(),
            c.id(),
            c.toString(),
            date,
            slot,
            EUK00361749_OrderStatus.PLACED,
            lines);
    EUK00361749_Store next = new EUK00361749_Store(store);
    for (EUK00361749_OrderLine l : lines) {
      EUK00361749_Product p = product(l.productId());
      next.put(p.withQuantity(p.quantity() - l.quantity()));
    }
    next.put(order);
    commit(next);
    return order;
  }

  public List<EUK00361749_Order> history() {
    EUK00361749_Account a = current();
    return store.orders().stream()
        .filter(o -> a instanceof EUK00361749_Admin || o.customerId().equals(a.id()))
        .sorted(
            Comparator.comparing(EUK00361749_Order::deliveryDate)
                .thenComparing(EUK00361749_Order::id))
        .toList();
  }

  private EUK00361749_Order accessible(String id) {
    EUK00361749_Account a = current();
    EUK00361749_Order o = store.order(id);
    if (o == null || (!(a instanceof EUK00361749_Admin) && !o.customerId().equals(a.id())))
      throw new IllegalArgumentException("Order not found or access denied.");
    return o;
  }

  public void cancel(String id) throws IOException {
    customer();
    EUK00361749_Order o = accessible(id);
    if (!o.status().cancellable())
      throw new IllegalArgumentException("Order cannot be cancelled at this status.");
    EUK00361749_Store next = new EUK00361749_Store(store);
    for (EUK00361749_OrderLine l : o.lines()) {
      EUK00361749_Product p = product(l.productId());
      next.put(p.withQuantity(Math.addExact(p.quantity(), l.quantity())));
    }
    next.put(o.withStatus(EUK00361749_OrderStatus.CANCELLED));
    commit(next);
  }

  public void advance(String id) throws IOException {
    admin();
    EUK00361749_Order o = accessible(id);
    EUK00361749_Store next = new EUK00361749_Store(store);
    next.put(o.withStatus(o.status().next()));
    commit(next);
  }

  public String invoice(String id) {
    EUK00361749_Order o = accessible(id);
    String heading =
        o.status() == EUK00361749_OrderStatus.CANCELLED
            ? "CANCELLED ORDER - HISTORICAL QUOTE ONLY"
            : "FRESHMART INVOICE";
    return heading
        + "\nOrder: "
        + o.id()
        + " | "
        + o.status()
        + "\nCustomer: "
        + o.customerDetails()
        + "\nDelivery: "
        + o.deliveryDate()
        + " | "
        + o.slot()
        + "\n"
        + pricing.calculate(o.lines())
        + (o.status() == EUK00361749_OrderStatus.CANCELLED
            ? "\nAMOUNT DUE: LKR 0.00 (cancelled)"
            : "\nPayment assumed cash on delivery; no electronic charge.");
  }
}
