package freshmart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * FreshMart's category-based pricing (concrete Strategy).
 *
 * <p>Assumptions for baskets that mix categories: the order must reach the highest minimum order
 * value among its categories, and the highest delivery fee among its categories is charged once.
 * Discount and tax are worked out per line using that line's category rate.
 */
public final class EUK00361749_CategoryPricing implements EUK00361749_PricingPolicy {

    /** Orders whose product total exceeds this amount receive the category discounts. */
    public static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("10000");

    @Override
    public EUK00361749_Invoice calculate(List<EUK00361749_OrderLine> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product.");
        }

        // Step 1: product total = sum of (unit price x quantity).
        // In the same pass, find the highest minimum order value and delivery fee.
        BigDecimal productTotal = BigDecimal.ZERO;
        BigDecimal requiredMinimum = BigDecimal.ZERO;
        BigDecimal deliveryFee = BigDecimal.ZERO;
        for (EUK00361749_OrderLine line : lines) {
            productTotal = productTotal.add(line.lineTotal());
            requiredMinimum = requiredMinimum.max(line.category().getMinimumOrderValue());
            deliveryFee = deliveryFee.max(line.category().getDeliveryFee());
        }
        if (productTotal.compareTo(requiredMinimum) < 0) {
            throw new IllegalArgumentException("The minimum order value for these categories is "
                    + EUK00361749_Invoice.formatMoney(requiredMinimum) + ".");
        }

        // Step 2: discount only when the order value EXCEEDS LKR 10,000.
        boolean discountEligible = productTotal.compareTo(DISCOUNT_THRESHOLD) > 0;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (EUK00361749_OrderLine line : lines) {
            BigDecimal lineDiscount = discountEligible
                    ? roundMoney(line.lineTotal().multiply(line.category().getDiscountRate()))
                    : BigDecimal.ZERO;
            // Step 4: tax on the discounted line value at the category's tax rate.
            BigDecimal lineTax = roundMoney(line.lineTotal().subtract(lineDiscount).multiply(line.category().getTaxRate()));
            totalDiscount = totalDiscount.add(lineDiscount);
            totalTax = totalTax.add(lineTax);
        }

        // Step 3 (delivery fee) and step 5: final payable = total - discount + delivery + tax.
        BigDecimal finalPayable = productTotal.subtract(totalDiscount).add(deliveryFee).add(totalTax);
        return new EUK00361749_Invoice(lines, roundMoney(productTotal), roundMoney(totalDiscount),
                roundMoney(deliveryFee), roundMoney(totalTax), roundMoney(finalPayable));
    }

    private static BigDecimal roundMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
