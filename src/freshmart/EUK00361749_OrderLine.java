package freshmart;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One product line inside an order. It copies the product's details at the time of ordering, so a
 * later price change does not alter an existing invoice.
 *
 * @param productId product ID at the time of ordering
 * @param name      product name at the time of ordering
 * @param category  product category
 * @param unitPrice price per unit at the time of ordering
 * @param quantity  units ordered (at least 1)
 */
public record EUK00361749_OrderLine(String productId, String name, EUK00361749_Category category,
                                    BigDecimal unitPrice, int quantity) {

    public EUK00361749_OrderLine {
        productId = EUK00361749_Validation.validateProductId(productId);
        name = EUK00361749_Validation.requireText(name, "Product name", 100);
        Objects.requireNonNull(category, "Category is required.");
        if (unitPrice == null || unitPrice.signum() <= 0 || unitPrice.scale() > 2) {
            throw new IllegalArgumentException("Unit price must be above 0 with up to 2 decimals.");
        }
        unitPrice = unitPrice.setScale(2);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be at least 1.");
        }
    }

    /** Creates a line from the current product record. */
    public static EUK00361749_OrderLine fromProduct(EUK00361749_Product product, int quantity) {
        return new EUK00361749_OrderLine(product.productId(), product.name(), product.category(),
                product.unitPrice(), quantity);
    }

    /** @return unit price x quantity. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
