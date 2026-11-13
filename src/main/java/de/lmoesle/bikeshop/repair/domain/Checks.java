package de.lmoesle.bikeshop.repair.domain;

import java.math.BigDecimal;

public final class Checks {
  private Checks() {}

  public static String text(String value, String name) {
    if (value == null || value.isBlank() || value.length() > 2000) {
      throw new IllegalArgumentException(name + " must contain 1–2000 characters");
    }
    return value.strip();
  }

  public static BigDecimal amount(BigDecimal value, String name, boolean positive) {
    if (value == null
        || value.signum() < 0
        || (positive && value.signum() == 0)
        || value.compareTo(new BigDecimal("100000")) > 0) {
      throw new IllegalArgumentException(
          name + " must be " + (positive ? "positive" : "nonnegative") + " and <= 100000");
    }
    return value;
  }
}
