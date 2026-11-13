package de.lmoesle.bikeshop.repair.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** All prices are EUR, including labour at the example's fixed hourly rate. */
public record Offer(BigDecimal amount) {
  public static final BigDecimal HOURLY_RATE = new BigDecimal("75.00");

  public Offer {
    amount = Checks.amount(amount, "offerAmount", true).setScale(2, RoundingMode.HALF_UP);
    if (amount.signum() == 0) throw new IllegalArgumentException("offerAmount rounds to zero");
  }

  public static Offer calculate(BigDecimal effortHours, BigDecimal materialCost) {
    final var effort = Checks.amount(effortHours, "effortHours", true);
    if (effort.compareTo(new BigDecimal("1000")) > 0)
      throw new IllegalArgumentException("effortHours must be <= 1000");
    final var materials = Checks.amount(materialCost, "materialCost", false);
    return new Offer(effort.multiply(HOURLY_RATE).add(materials));
  }

  public Offer revise(BigDecimal revisedAmount, String reason) {
    Checks.text(reason, "revisionReason");
    return new Offer(revisedAmount);
  }
}
