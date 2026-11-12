package freshmart;

import java.util.List;

public interface EUK00361749_PricingPolicy {
  EUK00361749_Invoice calculate(List<EUK00361749_OrderLine> lines);
}
