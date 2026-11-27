package freshmart;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the pricing strategy used for each category (the Strategy pattern's context lookup).
 *
 * <p>Data structure: an {@link EnumMap} keyed by category gives constant-time lookup and uses an
 * array internally, because the keys are a small fixed enum. The catalogue is immutable;
 * {@link #withStrategy} returns a new catalogue, so a different rate (for example a seasonal Dairy
 * promotion) can be swapped in without touching the billing decorators.
 */
public final class EUK00361749_PricingCatalogue {

    private final Map<EUK00361749_Category, EUK00361749_PricingStrategy> strategies;

    private EUK00361749_PricingCatalogue(Map<EUK00361749_Category, EUK00361749_PricingStrategy> strategies) {
        this.strategies = new EnumMap<>(strategies);
        for (EUK00361749_Category category : EUK00361749_Category.values()) {
            if (this.strategies.get(category) == null) {
                throw new IllegalArgumentException("No pricing strategy for " + category + ".");
            }
        }
    }

    /** @return the catalogue with the four strategies from Table 1 of the case study. */
    public static EUK00361749_PricingCatalogue standard() {
        Map<EUK00361749_Category, EUK00361749_PricingStrategy> table = new EnumMap<>(EUK00361749_Category.class);
        table.put(EUK00361749_Category.VEGETABLES, new EUK00361749_VegetablesPricing());
        table.put(EUK00361749_Category.FRUITS, new EUK00361749_FruitsPricing());
        table.put(EUK00361749_Category.DAIRY, new EUK00361749_DairyPricing());
        table.put(EUK00361749_Category.HOUSEHOLD_ESSENTIALS, new EUK00361749_HouseholdEssentialsPricing());
        return new EUK00361749_PricingCatalogue(table);
    }

    /** @return the strategy that prices goods in the given category. */
    public EUK00361749_PricingStrategy strategyFor(EUK00361749_Category category) {
        return strategies.get(Objects.requireNonNull(category, "Category is required."));
    }

    /** @return a copy of this catalogue in which one category uses a different strategy. */
    public EUK00361749_PricingCatalogue withStrategy(EUK00361749_Category category,
                                                     EUK00361749_PricingStrategy strategy) {
        Map<EUK00361749_Category, EUK00361749_PricingStrategy> copy = new EnumMap<>(strategies);
        copy.put(Objects.requireNonNull(category), Objects.requireNonNull(strategy));
        return new EUK00361749_PricingCatalogue(copy);
    }

    /**
     * Assumption for mixed baskets: the order must reach the highest minimum order value among
     * the categories it contains.
     */
    public BigDecimal requiredMinimum(List<EUK00361749_OrderLine> lines) {
        BigDecimal minimum = BigDecimal.ZERO;
        for (EUK00361749_OrderLine line : lines) {
            minimum = minimum.max(strategyFor(line.category()).getMinimumOrderValue());
        }
        return minimum;
    }
}
