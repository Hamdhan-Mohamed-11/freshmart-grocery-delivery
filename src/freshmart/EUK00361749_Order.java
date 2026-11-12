package freshmart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A customer's delivery order. The order owns its lines (composition): lines are created for this
 * order and never shared. Customer details are copied as text so the invoice keeps the address the
 * order was delivered to, even if the customer later edits their profile.
 *
 * <p>New orders are assembled with the nested {@link EUK00361749_OrderBuilder} (Builder pattern),
 * which collects lines one at a time before validating the whole order.
 *
 * @param orderId         sequential ID such as ORD0001
 * @param customerId      NIC of the customer who placed the order
 * @param customerDetails customer name, phone, address and email at the time of ordering
 * @param deliveryDate    chosen delivery date (tomorrow or later)
 * @param timeSlot        chosen delivery window
 * @param status          current stage of the order
 * @param lines           one or more product lines (unmodifiable)
 */
public record EUK00361749_Order(String orderId, String customerId, String customerDetails, LocalDate deliveryDate,
                                EUK00361749_TimeSlot timeSlot, EUK00361749_OrderStatus status,
                                List<EUK00361749_OrderLine> lines) {

    public EUK00361749_Order {
        orderId = EUK00361749_Validation.requireText(orderId, "Order ID", 40);
        customerId = EUK00361749_Validation.validateNic(customerId);
        customerDetails = EUK00361749_Validation.requireText(customerDetails, "Customer details", 700);
        Objects.requireNonNull(deliveryDate, "Delivery date is required.");
        Objects.requireNonNull(timeSlot, "Delivery time slot is required.");
        Objects.requireNonNull(status, "Order status is required.");
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

    /** @return a copy of this order at a new status. */
    public EUK00361749_Order withStatus(EUK00361749_OrderStatus newStatus) {
        return new EUK00361749_Order(orderId, customerId, customerDetails, deliveryDate, timeSlot, newStatus, lines);
    }

    @Override
    public String toString() {
        return String.format("%-8s | NIC %-12s | %s | %-22s | %d item(s) | %s",
                orderId, customerId, deliveryDate, timeSlot, lines.size(), status);
    }

    /** Builder for new orders (Builder design pattern, as in the module notes). */
    public static final class EUK00361749_OrderBuilder {

        private final String orderId;
        private final EUK00361749_Customer customer;
        private final List<EUK00361749_OrderLine> lines = new ArrayList<>();
        private LocalDate deliveryDate;
        private EUK00361749_TimeSlot timeSlot;

        /** Required values: the order ID and the customer placing it. */
        public EUK00361749_OrderBuilder(String orderId, EUK00361749_Customer customer) {
            this.orderId = orderId;
            this.customer = Objects.requireNonNull(customer, "Customer is required.");
        }

        public EUK00361749_OrderBuilder addLine(EUK00361749_OrderLine line) {
            lines.add(line);
            return this;
        }

        public EUK00361749_OrderBuilder deliverOn(LocalDate date, EUK00361749_TimeSlot slot) {
            this.deliveryDate = date;
            this.timeSlot = slot;
            return this;
        }

        /** Validates and creates the order with status PLACED. */
        public EUK00361749_Order build() {
            return new EUK00361749_Order(orderId, customer.getAccountId(), customer.toString(), deliveryDate,
                    timeSlot, EUK00361749_OrderStatus.PLACED, lines);
        }
    }
}
