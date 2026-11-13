package de.lmoesle.bikeshop.repair.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskCompletionTest {
  @Test
  void allowsOnlyTaskSpecificVariables() {
    assertThat(
            TaskCompletion.variables(
                "AssessDamage",
                Map.of("damageDescription", "Bremse", "effortHours", 2, "materialCost", 10)))
        .containsOnlyKeys("damageDescription", "effortHours", "materialCost");
    assertThat(TaskCompletion.variables("ReviewOffer", Map.of("reviewDecision", "APPROVE")))
        .containsEntry("reviewDecision", "APPROVE");
    assertThat(
            TaskCompletion.variables(
                "ReviseOffer", Map.of("offerAmount", 50, "revisionReason", "Kulanz")))
        .containsKey("offerAmount");
    assertThat(TaskCompletion.variables("RepairBicycle", Map.of("repairNotes", "Erledigt")))
        .containsKey("repairNotes");
  }

  @Test
  void rejectsUnknownMissingWrongTypedAndNegativeInputs() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            TaskCompletion.variables(
                "ReviewOffer", Map.of("reviewDecision", "APPROVE", "repairId", "evil")));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> TaskCompletion.variables("ReviewOffer", Map.of("reviewDecision", "invalid")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TaskCompletion.variables("ReviewOffer", Map.of("reviewDecision", 1)));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            TaskCompletion.variables(
                "ReviseOffer", Map.of("offerAmount", -1, "revisionReason", "x")));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            TaskCompletion.variables(
                "ReviseOffer", Map.of("offerAmount", "50", "revisionReason", "x")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TaskCompletion.variables("RepairBicycle", Map.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TaskCompletion.variables("unknown", Map.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TaskCompletion.variables("RepairBicycle", null));
  }
}
