package freshmart;

/**
 * Concrete strategy for the Fruits category (Table 1): delivery fee LKR 350, minimum order
 * value LKR 2500, discount 5% and tax 5%.
 */
public final class EUK00361749_FruitsPricing extends EUK00361749_RatePricingStrategy {

    public EUK00361749_FruitsPricing() {
        super("350", "2500", "0.05", "0.05");
    }
}
