package freshmart;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step 2 of the final bill. When the product total EXCEEDS LKR 10,000, every line receives the
 * discount of its own category strategy; otherwise the discount is zero. The discount is taken
 * from the product value before tax.
 */
public final class EUK00361749_DiscountDecorator extends EUK00361749_BillDecorator {

    /** Orders whose product total is above this value receive the category discounts. */
    public static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("10000.00");

    // Product ID -> discount on that line; LinkedHashMap keeps the invoice line order.
    private final Map<String, BigDecimal> discountByProductId = new LinkedHashMap<>();
    private final BigDecimal totalDiscount;

    public EUK00361749_DiscountDecorator(EUK00361749_Bill wrappedBill, EUK00361749_PricingCatalogue catalogue) {
        super(wrappedBill, catalogue);
        boolean eligible = wrappedBill.getProductTotal().compareTo(DISCOUNT_THRESHOLD) > 0;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (EUK00361749_OrderLine line : wrappedBill.getLines()) {
            BigDecimal lineDiscount = eligible
                    ? strategyFor(line).calculateDiscount(line.lineTotal())
                    : BigDecimal.ZERO.setScale(2);
            discountByProductId.put(line.productId(), lineDiscount);
            total = total.add(lineDiscount);
        }
        this.totalDiscount = total;
    }

    @Override
    protected String getLayerName() {
        return "Discount";
    }

    @Override
    public BigDecimal getDiscount() {
        return getWrappedBill().getDiscount().add(totalDiscount);
    }

    @Override
    public BigDecimal getDiscountFor(EUK00361749_OrderLine line) {
        return getWrappedBill().getDiscountFor(line)
                .add(discountByProductId.getOrDefault(line.productId(), BigDecimal.ZERO));
    }

    @Override
    public BigDecimal getAmount() {
        return getWrappedBill().getAmount().subtract(totalDiscount);
    }
}
