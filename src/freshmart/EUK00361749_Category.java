package freshmart;

/**
 * The four product categories named in the case study. The category only identifies a group of
 * products; its delivery fee, minimum order value, discount and tax are supplied by the matching
 * pricing strategy in {@link EUK00361749_PricingCatalogue} (Strategy pattern), so rates can change
 * without editing this enum or the billing code.
 */
public enum EUK00361749_Category {
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    DAIRY("Dairy"),
    HOUSEHOLD_ESSENTIALS("Household Essentials");

    private final String displayName;

    EUK00361749_Category(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
