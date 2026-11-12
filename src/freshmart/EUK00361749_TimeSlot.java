package freshmart;

/** Delivery time windows a customer can choose from. */
public enum EUK00361749_TimeSlot {
    MORNING_09_12("Morning 09:00-12:00"),
    AFTERNOON_12_16("Afternoon 12:00-16:00"),
    EVENING_16_20("Evening 16:00-20:00");

    private final String displayName;

    EUK00361749_TimeSlot(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
