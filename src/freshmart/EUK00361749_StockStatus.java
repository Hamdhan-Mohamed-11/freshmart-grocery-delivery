package freshmart;

/** Stock levels from the case study. Only IN_STOCK products can be ordered. */
public enum EUK00361749_StockStatus {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock");

    private final String displayName;

    EUK00361749_StockStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
