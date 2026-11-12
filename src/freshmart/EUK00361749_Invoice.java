package freshmart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable result of a pricing calculation, plus formatting for the console and text export.
 *
 * @param lines        product lines that were priced
 * @param productTotal sum of unit price x quantity
 * @param discount     category discounts (0 unless the order exceeds LKR 10,000)
 * @param deliveryFee  delivery charge
 * @param tax          category taxes
 * @param finalPayable product total - discount + delivery + tax
 */
public record EUK00361749_Invoice(List<EUK00361749_OrderLine> lines, BigDecimal productTotal, BigDecimal discount,
                                  BigDecimal deliveryFee, BigDecimal tax, BigDecimal finalPayable) {

    private static final String DOUBLE_LINE = "=".repeat(92);
    private static final String SINGLE_LINE = "-".repeat(92);

    public EUK00361749_Invoice {
        lines = List.copyOf(lines);
    }

    /** Product table and totals only (used for the price preview before ordering). */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append(String.format("%-8s %-24s %-20s %14s %5s %15s%n",
                "ID", "Product", "Category", "Unit price", "Qty", "Line total"));
        text.append(SINGLE_LINE).append('\n');
        for (EUK00361749_OrderLine line : lines) {
            text.append(String.format("%-8s %-24s %-20s %14s %5d %15s%n", line.productId(), line.name(),
                    line.category(), formatMoney(line.unitPrice()), line.quantity(), formatMoney(line.lineTotal())));
        }
        text.append(SINGLE_LINE).append('\n')
            .append(totalRow("Product total", formatMoney(productTotal)))
            .append(totalRow("Discount applied", "- " + formatMoney(discount)))
            .append(totalRow("Delivery fee", formatMoney(deliveryFee)))
            .append(totalRow("Tax added", formatMoney(tax)))
            .append(DOUBLE_LINE).append('\n')
            .append(totalRow("FINAL PAYABLE AMOUNT", formatMoney(finalPayable)));
        return text.toString();
    }

    /** Full invoice document: heading, customer and delivery details, then the product table. */
    public String toDocument(EUK00361749_Order order) {
        boolean cancelled = order.status() == EUK00361749_OrderStatus.CANCELLED;
        StringBuilder text = new StringBuilder();
        text.append(DOUBLE_LINE).append('\n')
            .append(String.format("%-60s%32s%n",
                    cancelled ? "FRESHMART - CANCELLED ORDER (historical copy)" : "FRESHMART GROCERY DELIVERY - INVOICE",
                    "Order " + order.orderId()))
            .append(DOUBLE_LINE).append('\n')
            .append("Customer : ").append(order.customerDetails()).append('\n')
            .append("Delivery : ").append(order.deliveryDate()).append(" | ").append(order.timeSlot()).append('\n')
            .append("Status   : ").append(order.status()).append('\n')
            .append(SINGLE_LINE).append('\n')
            .append(this);
        if (cancelled) {
            text.append(totalRow("AMOUNT DUE", "LKR 0.00 (cancelled)"));
        } else {
            text.append("Payment: cash on delivery.\n");
        }
        return text.toString();
    }

    private static String totalRow(String label, String value) {
        return String.format("%73s %18s%n", label, value);
    }

    /** Formats an amount as "LKR 12,345.00". */
    public static String formatMoney(BigDecimal amount) {
        return String.format("LKR %,.2f", amount);
    }
}
