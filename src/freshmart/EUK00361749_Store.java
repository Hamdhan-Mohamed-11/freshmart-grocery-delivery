package freshmart;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Versioned, encoded snapshot; candidate stores enable commit-before-publish updates. */
public final class EUK00361749_Store {
  private final Map<String, EUK00361749_Account> accounts = new LinkedHashMap<>();
  private final Map<String, EUK00361749_Product> products = new LinkedHashMap<>();
  private final Map<String, EUK00361749_Order> orders = new LinkedHashMap<>();

  public EUK00361749_Store() {}

  public EUK00361749_Store(EUK00361749_Store other) {
    accounts.putAll(other.accounts);
    products.putAll(other.products);
    orders.putAll(other.orders);
  }

  public List<EUK00361749_Account> accounts() {
    return List.copyOf(accounts.values());
  }

  public List<EUK00361749_Product> products() {
    return List.copyOf(products.values());
  }

  public List<EUK00361749_Order> orders() {
    return List.copyOf(orders.values());
  }

  public EUK00361749_Account account(String id) {
    return accounts.get(id);
  }

  public EUK00361749_Product product(String id) {
    return products.get(EUK00361749_Validation.productId(id));
  }

  public EUK00361749_Order order(String id) {
    return orders.get(id);
  }

  void put(EUK00361749_Account a) {
    accounts.put(a.id(), a);
  }

  void put(EUK00361749_Product p) {
    products.put(p.id(), p);
  }

  void put(EUK00361749_Order o) {
    orders.put(o.id(), o);
  }

  void removeProduct(String id) {
    products.remove(id);
  }

  private static String row(String... fields) {
    return String.join(
        "\t",
        Arrays.stream(fields)
            .map(s -> Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))
            .toArray(String[]::new));
  }

  public void save(Path file) throws IOException {
    List<String> data = new ArrayList<>();
    data.add("FRESHMART-1");
    for (EUK00361749_Account a : accounts()) {
      if (a instanceof EUK00361749_Customer c)
        data.add(row("C", c.id(), c.name(), c.phone(), c.address(), c.email(), c.passwordHash()));
      else data.add(row("A", a.name(), a.email(), a.passwordHash()));
    }
    for (EUK00361749_Product p : products())
      data.add(
          row("P", p.id(), p.name(), p.category().name(), p.price().toString(), "" + p.quantity()));
    for (EUK00361749_Order o : orders()) {
      data.add(
          row(
              "O",
              o.id(),
              o.customerId(),
              o.customerDetails(),
              o.deliveryDate().toString(),
              o.slot().name(),
              o.status().name()));
      for (EUK00361749_OrderLine l : o.lines())
        data.add(
            row(
                "L",
                o.id(),
                l.productId(),
                l.name(),
                l.category().name(),
                l.unitPrice().toString(),
                "" + l.quantity()));
    }
    Path target = file.toAbsolutePath();
    Files.createDirectories(target.getParent());
    Path temp = Files.createTempFile(target.getParent(), "freshmart-", ".tmp");
    try {
      Files.write(temp, data, StandardCharsets.UTF_8);
      try {
        Files.move(
            temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public static EUK00361749_Store load(Path file) throws IOException {
    EUK00361749_Store s = new EUK00361749_Store();
    if (!Files.exists(file)) return s;
    List<String> data = Files.readAllLines(file, StandardCharsets.UTF_8);
    if (data.isEmpty() || !data.get(0).equals("FRESHMART-1"))
      throw new IOException("Unsupported or damaged FreshMart file.");
    Map<String, String[]> headers = new LinkedHashMap<>();
    Map<String, List<EUK00361749_OrderLine>> lines = new LinkedHashMap<>();
    Set<String> emails = new HashSet<>();
    try {
      for (String raw : data.subList(1, data.size())) {
        String[] f =
            Arrays.stream(raw.split("\t", -1))
                .map(v -> new String(Base64.getDecoder().decode(v), StandardCharsets.UTF_8))
                .toArray(String[]::new);
        switch (f[0]) {
          case "A", "C" -> {
            EUK00361749_Account a;
            if (f[0].equals("A") && f.length == 4) a = new EUK00361749_Admin(f[1], f[2], f[3]);
            else if (f[0].equals("C") && f.length == 7)
              a = new EUK00361749_Customer(f[1], f[2], f[3], f[4], f[5], f[6]);
            else throw new IllegalArgumentException();
            if (s.account(a.id()) != null || !emails.add(a.email()))
              throw new IllegalArgumentException();
            s.put(a);
          }
          case "P" -> {
            if (f.length != 6 || s.product(f[1]) != null) throw new IllegalArgumentException();
            s.put(
                new EUK00361749_Product(
                    f[1],
                    f[2],
                    EUK00361749_Category.valueOf(f[3]),
                    new BigDecimal(f[4]),
                    Integer.parseInt(f[5])));
          }
          case "O" -> {
            if (f.length != 7 || headers.putIfAbsent(f[1], f) != null)
              throw new IllegalArgumentException();
          }
          case "L" -> {
            if (f.length != 7) throw new IllegalArgumentException();
            lines
                .computeIfAbsent(f[1], k -> new ArrayList<>())
                .add(
                    new EUK00361749_OrderLine(
                        f[2],
                        f[3],
                        EUK00361749_Category.valueOf(f[4]),
                        new BigDecimal(f[5]),
                        Integer.parseInt(f[6])));
          }
          default -> throw new IllegalArgumentException();
        }
      }
      if (!lines.keySet().equals(headers.keySet())) throw new IllegalArgumentException();
      if (!s.accounts.isEmpty() && !(s.account("ADMIN") instanceof EUK00361749_Admin))
        throw new IllegalArgumentException();
      for (String[] f : headers.values()) {
        if (!(s.account(f[2]) instanceof EUK00361749_Customer))
          throw new IllegalArgumentException();
        EUK00361749_Order o =
            new EUK00361749_Order(
                f[1],
                f[2],
                f[3],
                LocalDate.parse(f[4]),
                EUK00361749_TimeSlot.valueOf(f[5]),
                EUK00361749_OrderStatus.valueOf(f[6]),
                lines.get(f[1]));
        for (EUK00361749_OrderLine l : o.lines())
          if (s.product(l.productId()) == null) throw new IllegalArgumentException();
        new EUK00361749_CategoryPricing().calculate(o.lines());
        s.put(o);
      }
      return s;
    } catch (RuntimeException ex) {
      throw new IOException("Invalid stored data; original file preserved.", ex);
    }
  }
}
