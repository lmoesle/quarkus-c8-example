package de.lmoesle.bikeshop.repair.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Allowlist independent of the transport: no caller can overwrite identity/customer variables. */
public final class TaskCompletion {
  private TaskCompletion() {}

  public static Map<String, Object> variables(String type, Map<String, Object> input) {
    if (input == null) throw new IllegalArgumentException("payload is required");
    return switch (type) {
      case "AssessDamage" -> assess(input);
      case "ReviewOffer" -> review(input);
      case "ReviseOffer" -> revise(input);
      case "RepairBicycle" -> repair(input);
      default -> throw new IllegalArgumentException("Unsupported task type: " + type);
    };
  }

  private static Map<String, Object> assess(Map<String, Object> input) {
    keys(input, Set.of("damageDescription", "effortHours", "materialCost"));
    Offer.calculate(number(input, "effortHours"), number(input, "materialCost"));
    return Map.of(
        "damageDescription",
        text(input, "damageDescription"),
        "effortHours",
        Checks.amount(number(input, "effortHours"), "effortHours", true),
        "materialCost",
        Checks.amount(number(input, "materialCost"), "materialCost", false));
  }

  private static Map<String, Object> review(Map<String, Object> input) {
    keys(input, Set.of("reviewDecision"));
    final var decision = text(input, "reviewDecision");
    if (!Set.of("APPROVE", "REVISE").contains(decision)) {
      throw new IllegalArgumentException("reviewDecision must be APPROVE or REVISE");
    }
    return Map.of("reviewDecision", decision);
  }

  private static Map<String, Object> revise(Map<String, Object> input) {
    keys(input, Set.of("offerAmount", "revisionReason"));
    final var reason = text(input, "revisionReason");
    final var offer = new Offer(number(input, "offerAmount"));
    return Map.of("offerAmount", offer.amount(), "revisionReason", reason);
  }

  private static Map<String, Object> repair(Map<String, Object> input) {
    keys(input, Set.of("repairNotes"));
    return Map.of("repairNotes", text(input, "repairNotes"));
  }

  private static void keys(Map<String, Object> input, Set<String> expected) {
    if (!input.keySet().equals(expected))
      throw new IllegalArgumentException("Expected exactly: " + expected);
  }

  private static String text(Map<String, Object> input, String key) {
    if (!(input.get(key) instanceof String value))
      throw new IllegalArgumentException(key + " must be text");
    return Checks.text(value, key);
  }

  private static BigDecimal number(Map<String, Object> input, String key) {
    if (!(input.get(key) instanceof Number value))
      throw new IllegalArgumentException(key + " must be numeric");
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException failure) {
      throw new IllegalArgumentException(key + " must be finite", failure);
    }
  }
}
