package freshmart;

import java.math.BigDecimal;

/**
 * Step 3 of the final bill: adds one delivery fee per order. Assumption for mixed baskets: the
 * highest delivery fee among the categories in the order is charged, once.
 */
public final class EUK00361749_DeliveryFeeDecorator extends EUK00361749_BillDecorator {

    private final BigDecimal deliveryFee;

    public EUK00361749_DeliveryFeeDecorator(EUK00361749_Bill wrappedBill, EUK00361749_PricingCatalogue catalogue) {
        super(wrappedBill, catalogue);
        BigDecimal highestFee = BigDecimal.ZERO.setScale(2);
        for (EUK00361749_OrderLine line : wrappedBill.getLines()) {
            highestFee = highestFee.max(strategyFor(line).getDeliveryFee());
        }
        this.deliveryFee = highestFee;
    }

    @Override
    protected String getLayerName() {
        return "Delivery fee";
    }

    @Override
    public BigDecimal getDeliveryFee() {
        return getWrappedBill().getDeliveryFee().add(deliveryFee);
    }

    @Override
    public BigDecimal getAmount() {
        return getWrappedBill().getAmount().add(deliveryFee);
    }
}
