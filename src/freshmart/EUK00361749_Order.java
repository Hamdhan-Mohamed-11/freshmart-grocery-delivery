package freshmart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A customer's delivery order and the <em>context</em> of the State pattern.
 *
 * <p>The order owns its lines (composition): they are created for this order and never shared.
 * Customer details are copied as text so the invoice keeps the address the order was delivered
 * to, even if the customer later edits their profile. Orders are immutable: a status change
 * returns a new Order whose state was chosen by the current state object, which lets the service
 * save the change before it becomes visible.
 *
 * <p>New orders are created only through the nested {@link EUK00361749_OrderBuilder} (Builder
 * pattern), which validates every order rule before the order exists.
 *
 * @param orderId         sequential ID such as ORD0001
 * @param customerId      NIC of the customer who placed the order
 * @param customerDetails customer name, phone, address and email at the time of ordering
 * @param deliveryDate    chosen delivery date (tomorrow or later)
 * @param timeSlot        chosen delivery window
 * @param state           current lifecycle state (State pattern)
 * @param lines           one or more product lines (unmodifiable)
 */
public record EUK00361749_Order(String orderId, String customerId, String customerDetails, LocalDate deliveryDate,
                                EUK00361749_TimeSlot timeSlot, EUK00361749_OrderState state,
                                List<EUK00361749_OrderLine> lines) {

    /** Invariants that must hold for every order, including orders loaded from the data file. */
    public EUK00361749_Order {
        orderId = EUK00361749_Validation.requireText(orderId, "Order ID", 40);
        customerId = EUK00361749_Validation.validateNic(customerId);
        customerDetails = EUK00361749_Validation.requireText(customerDetails, "Customer details", 700);
        Objects.requireNonNull(deliveryDate, "Delivery date is required.");
        Objects.requireNonNull(timeSlot, "Delivery time slot is required.");
        Objects.requireNonNull(state, "Order status is required.");
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product.");
        }
        // A HashSet detects a repeated product ID in O(1) per line.
        Set<String> seenProductIds = new HashSet<>();
        for (EUK00361749_OrderLine line : lines) {
            if (!seenProductIds.add(line.productId())) {
                throw new IllegalArgumentException("Product " + line.productId() + " appears twice in the order.");
            }
        }
    }

    /** State pattern: the current state decides the next one (or refuses). */
    public EUK00361749_Order advance() {
        return withState(state.advance());
    }

    /** State pattern: the current state allows cancellation only before Out for Delivery. */
    public EUK00361749_Order cancel() {
        return withState(state.cancel());
    }

    private EUK00361749_Order withState(EUK00361749_OrderState newState) {
        return new EUK00361749_Order(orderId, customerId, customerDetails, deliveryDate, timeSlot, newState, lines);
    }

    @Override
    public String toString() {
        return String.format("%-8s | NIC %-12s | %s | %-22s | %d item(s) | %s",
                orderId, customerId, deliveryDate, timeSlot, lines.size(), state);
    }

    /**
     * Builder pattern: assembles a delivery order step by step (customer, products and quantities,
     * delivery date, time slot, initial status) and validates the order rules before the order is
     * created.
     *
     * <pre>
     *   new OrderBuilder(catalogue, today)
     *       .forCustomer(customer)
     *       .addProduct(product, 2)        // repeat for every product
     *       .deliverOn(date)
     *       .inTimeSlot(slot)
     *       .build("ORD0001");             // validate() runs first; status starts as Placed
     * </pre>
     */
    public static final class EUK00361749_OrderBuilder {

        private final EUK00361749_PricingCatalogue catalogue;
        private final LocalDate today;
        // Product ID -> line: keeps basket order and rejects a product added twice in O(1).
        private final Map<String, EUK00361749_OrderLine> linesByProductId = new LinkedHashMap<>();
        private EUK00361749_Customer customer;
        private LocalDate deliveryDate;
        private EUK00361749_TimeSlot timeSlot;

        /**
         * @param catalogue pricing strategies, used to check the minimum order value
         * @param today     the store's current date (from the service clock)
         */
        public EUK00361749_OrderBuilder(EUK00361749_PricingCatalogue catalogue, LocalDate today) {
            this.catalogue = Objects.requireNonNull(catalogue, "Pricing catalogue is required.");
            this.today = Objects.requireNonNull(today, "Today's date is required.");
        }

        public EUK00361749_OrderBuilder forCustomer(EUK00361749_Customer customer) {
            this.customer = customer;
            return this;
        }

        /**
         * Adds one product. Rule: only In Stock products can be ordered, the quantity must be at
         * least 1 and no more than the stock held, and a product may appear only once.
         */
        public EUK00361749_OrderBuilder addProduct(EUK00361749_Product product, int quantity) {
            Objects.requireNonNull(product, "Product is required.");
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            if (product.stockStatus() != EUK00361749_StockStatus.IN_STOCK) {
                throw new IllegalArgumentException(product.productId() + " is " + product.stockStatus()
                        + "; only In Stock products can be ordered.");
            }
            if (quantity > product.stockQuantity()) {
                throw new IllegalArgumentException("Only " + product.stockQuantity() + " of " + product.productId()
                        + " available.");
            }
            if (linesByProductId.containsKey(product.productId())) {
                throw new IllegalArgumentException("Product " + product.productId() + " is in the basket twice.");
            }
            linesByProductId.put(product.productId(), EUK00361749_OrderLine.fromProduct(product, quantity));
            return this;
        }

        public EUK00361749_OrderBuilder deliverOn(LocalDate date) {
            this.deliveryDate = date;
            return this;
        }

        public EUK00361749_OrderBuilder inTimeSlot(EUK00361749_TimeSlot slot) {
            this.timeSlot = slot;
            return this;
        }

        /**
         * Checks the whole order against the business rules without creating it (also used for the
         * price preview).
         *
         * @return the validated order lines
         */
        public List<EUK00361749_OrderLine> validate() {
            if (customer == null) {
                throw new IllegalArgumentException("Only a registered customer can place an order.");
            }
            if (linesByProductId.isEmpty()) {
                throw new IllegalArgumentException("An order must contain at least one product.");
            }
            if (deliveryDate == null) {
                throw new IllegalArgumentException("Choose a delivery date.");
            }
            if (deliveryDate.isBefore(today)) {
                throw new IllegalArgumentException("The delivery date is in the past; choose "
                        + today.plusDays(1) + " or later.");
            }
            if (deliveryDate.isEqual(today)) {
                throw new IllegalArgumentException("Same-day delivery is not available; choose "
                        + today.plusDays(1) + " or later.");
            }
            if (timeSlot == null) {
                throw new IllegalArgumentException("Choose a delivery time slot.");
            }
            List<EUK00361749_OrderLine> lines = new ArrayList<>(linesByProductId.values());
            BigDecimal productTotal = BigDecimal.ZERO;
            for (EUK00361749_OrderLine line : lines) {
                productTotal = productTotal.add(line.lineTotal());
            }
            BigDecimal minimum = catalogue.requiredMinimum(lines);
            if (productTotal.compareTo(minimum) < 0) {
                throw new IllegalArgumentException("The minimum order value for these categories is "
                        + EUK00361749_Invoice.formatMoney(minimum) + ".");
            }
            return List.copyOf(lines);
        }

        /** Validates, then creates the order with the initial status Placed. */
        public EUK00361749_Order build(String orderId) {
            List<EUK00361749_OrderLine> lines = validate();
            return new EUK00361749_Order(orderId, customer.getAccountId(), customer.toString(), deliveryDate,
                    timeSlot, new EUK00361749_PlacedState(), lines);
        }
    }
}
