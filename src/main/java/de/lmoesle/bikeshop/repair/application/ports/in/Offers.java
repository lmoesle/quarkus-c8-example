package de.lmoesle.bikeshop.repair.application.ports.in;

import de.lmoesle.bikeshop.repair.domain.Offer;
import java.math.BigDecimal;

public interface Offers {
  Offer create(Estimate command);

  void send(Send command);

  record Estimate(BigDecimal effortHours, BigDecimal materialCost) {}

  record Send(String repairId, String customerName, String customerEmail, Offer offer) {}
}
