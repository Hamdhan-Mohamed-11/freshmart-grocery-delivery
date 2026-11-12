package freshmart;

import java.math.BigDecimal;

public enum EUK00361749_Category {
  VEGETABLES(300, 2000, "0.05", "0.05"),
  FRUITS(350, 2500, "0.05", "0.05"),
  DAIRY(400, 3000, "0.08", "0.08"),
  HOUSEHOLD_ESSENTIALS(500, 4000, "0.10", "0.10");
  private final BigDecimal delivery, minimum, discount, tax;

  EUK00361749_Category(int delivery, int minimum, String discount, String tax) {
    this.delivery = BigDecimal.valueOf(delivery);
    this.minimum = BigDecimal.valueOf(minimum);
    this.discount = new BigDecimal(discount);
    this.tax = new BigDecimal(tax);
  }

  public BigDecimal delivery() {
    return delivery;
  }

  public BigDecimal minimum() {
    return minimum;
  }

  public BigDecimal discount() {
    return discount;
  }

  public BigDecimal tax() {
    return tax;
  }
}
