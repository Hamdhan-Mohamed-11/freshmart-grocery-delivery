package freshmart;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An immutable product record. Changing stock or price creates a new record with the same ID.
 *
 * <p>The stock status is normally worked out from the quantity (0 = Out of Stock, 1-5 = Low Stock,
 * more = In Stock), but the administrator may also set it directly, for example to mark a product
 * Out of Stock while items are being checked. A status that contradicts the quantity (In Stock with
 * zero items) is rejected.
 *
 * @param productId     unique ID in upper case
 * @param name          product name
 * @param category      category that sets delivery fee, discount and tax
 * @param unitPrice     price per unit in LKR (max 2 decimals)
 * @param stockQuantity units available
 * @param stockStatus   In Stock / Low Stock / Out of Stock
 */
public record EUK00361749_Product(String productId, String name, EUK00361749_Category category,
                                  BigDecimal unitPrice, int stockQuantity, EUK00361749_StockStatus stockStatus) {

    /** Quantities at or below this level count as Low Stock. */
    public static final int LOW_STOCK_LIMIT = 5;
    private static final BigDecimal MAXIMUM_PRICE = new BigDecimal("1000000");

    /** Validating constructor run for every new product record. */
    public EUK00361749_Product {
        productId = EUK00361749_Validation.validateProductId(productId);
        name = EUK00361749_Validation.requireText(name, "Product name", 100);
        Objects.requireNonNull(category, "Category is required.");
        if (unitPrice == null || unitPrice.signum() <= 0 || unitPrice.scale() > 2
                || unitPrice.compareTo(MAXIMUM_PRICE) > 0) {
            throw new IllegalArgumentException("Unit price must be above 0, at most LKR 1,000,000, with up to 2 decimals.");
        }
        unitPrice = unitPrice.setScale(2);
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        Objects.requireNonNull(stockStatus, "Stock status is required.");
        if (stockQuantity == 0 && stockStatus != EUK00361749_StockStatus.OUT_OF_STOCK) {
            throw new IllegalArgumentException("A product with zero stock must be Out of Stock.");
        }
    }

    /** Creates a product whose status is calculated from the quantity. */
    public EUK00361749_Product(String productId, String name, EUK00361749_Category category,
                               BigDecimal unitPrice, int stockQuantity) {
        this(productId, name, category, unitPrice, stockQuantity, statusForQuantity(stockQuantity));
    }

    /** Automatic stock status for a given quantity. */
    public static EUK00361749_StockStatus statusForQuantity(int quantity) {
        if (quantity == 0) {
            return EUK00361749_StockStatus.OUT_OF_STOCK;
        }
        return quantity <= LOW_STOCK_LIMIT ? EUK00361749_StockStatus.LOW_STOCK : EUK00361749_StockStatus.IN_STOCK;
    }

    /** @return a copy with a new quantity; the status is recalculated automatically. */
    public EUK00361749_Product withStockQuantity(int newQuantity) {
        return new EUK00361749_Product(productId, name, category, unitPrice, newQuantity);
    }

    /** @return a copy with a status chosen by the administrator. */
    public EUK00361749_Product withStockStatus(EUK00361749_StockStatus newStatus) {
        return new EUK00361749_Product(productId, name, category, unitPrice, stockQuantity, newStatus);
    }

    @Override
    public String toString() {
        return String.format("%-8s | %-24s | %-20s | LKR %,10.2f | %8d | %s",
                productId, name, category, unitPrice, stockQuantity, stockStatus);
    }
}
