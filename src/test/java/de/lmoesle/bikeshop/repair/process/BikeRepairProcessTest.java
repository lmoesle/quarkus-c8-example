package de.lmoesle.bikeshop.repair.process;

import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.byElementId;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTest;
import io.camunda.process.test.api.CamundaProcessTestContext;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@CamundaProcessTest
class BikeRepairProcessTest {
  private CamundaClient client;
  private CamundaProcessTestContext context;

  @BeforeEach
  void deploy() {
    client
        .newDeployResourceCommand()
        .addResourceFromClasspath("bpmn/bike-repair.bpmn")
        .addResourceFromClasspath("forms/assess-damage.form")
        .addResourceFromClasspath("forms/review-offer.form")
        .addResourceFromClasspath("forms/revise-offer.form")
        .addResourceFromClasspath("forms/repair-bicycle.form")
        .send()
        .join();
  }

  @Test
  void approvedAcceptedRepairedAndLosingEventsCanceled() {
    final var id = UUID.randomUUID().toString();
    final var instance = offer(id, Map.of("reviewDecision", "APPROVE"));
    waitForDecision(instance);
    message("BikeRepairOfferAccepted", id);
    assertThat(instance)
        .hasActiveElements("RepairBicycle")
        .isNotWaitingForMessage("BikeRepairOfferRejected", id);
    assertThat(byElementId("RepairBicycle")).hasCandidateGroup("technicians");
    message("BikeRepairOfferRejected", id);
    context.increaseTime(Duration.ofDays(15));
    assertThat(instance)
        .hasActiveElements("RepairBicycle")
        .hasNotActivatedElements("ReturnedRejected", "ReturnedExpired");
    context.completeUserTask("RepairBicycle", Map.of("repairNotes", "Erledigt"));
    assertThat(instance).isCompleted().hasCompletedElements("ReturnedRepaired");
  }

  @Test
  void revisionPricePropagatesToSendJob() {
    final var instance = offer(UUID.randomUUID().toString(), Map.of("reviewDecision", "REVISE"));
    assertThat(instance).hasActiveElements("ReviseOffer");
    assertThat(byElementId("ReviseOffer")).hasCandidateGroup("customer-service");
    context.completeUserTask("ReviseOffer", Map.of("offerAmount", 99, "revisionReason", "Kulanz"));
    assertThat(instance).hasActiveElements("SendOffer").hasVariable("offerAmount", 99);
    context.completeJob("send-offer");
    assertThat(instance)
        .hasActiveElements("AwaitCustomerDecision")
        .isWaitingForMessage("BikeRepairOfferAccepted");
  }

  @Test
  void rejectionDoesNotRepairAndCancelsAcceptance() {
    final var id = UUID.randomUUID().toString();
    final var instance = offer(id, Map.of("reviewDecision", "APPROVE"));
    waitForDecision(instance);
    message("BikeRepairOfferRejected", id);
    assertThat(instance)
        .isCompleted()
        .hasCompletedElements("ReturnedRejected")
        .hasNotActivatedElements("RepairBicycle")
        .isNotWaitingForMessage("BikeRepairOfferAccepted", id);
  }

  @Test
  void twoWeekTimerDoesNotRepair() {
    final var instance = offer(UUID.randomUUID().toString(), Map.of("reviewDecision", "APPROVE"));
    waitForDecision(instance); // Timer subscription is active before moving the clock.
    context.increaseTime(Duration.ofDays(13));
    assertThat(instance).isActive().hasActiveElements("AwaitCustomerDecision");
    context.increaseTime(Duration.ofDays(1).plusSeconds(1));
    assertThat(instance)
        .isCompleted()
        .hasCompletedElements("ReturnedExpired")
        .hasNotActivatedElements("RepairBicycle");
  }

  @Test
  void messagesOnlyCorrelateToMatchingRepair() {
    final var firstId = UUID.randomUUID().toString();
    final var first = offer(firstId, Map.of("reviewDecision", "APPROVE"));
    waitForDecision(first);
    final var secondId = UUID.randomUUID().toString();
    final var second = offer(secondId, Map.of("reviewDecision", "APPROVE"));
    waitForDecision(second);
    message("BikeRepairOfferAccepted", secondId);
    assertThat(second).hasActiveElements("RepairBicycle");
    assertThat(first)
        .isWaitingForMessage("BikeRepairOfferAccepted", firstId)
        .hasNotActivatedElements("RepairBicycle");
  }

  static Stream<Map<String, Object>> unsafeReviews() {
    final var nullValue = new java.util.HashMap<String, Object>();
    nullValue.put("reviewDecision", null);
    return Stream.of(
        Map.of(),
        nullValue,
        Map.of("reviewDecision", 42),
        Map.of("reviewDecision", "invalid"),
        Map.of("reviewDecision", false));
  }

  @ParameterizedTest
  @MethodSource("unsafeReviews")
  void unsafeReviewDefaultsToRevision(Map<String, Object> review) {
    final var instance = offer(UUID.randomUUID().toString(), review);
    assertThat(instance)
        .hasActiveElements("ReviseOffer")
        .hasNotActivatedElements("SendOffer")
        .hasNoActiveIncidents();
  }

  private ProcessInstanceEvent offer(String id, Map<String, Object> review) {
    final var instance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("BikeRepair")
            .latestVersion()
            .variables(Map.of("repairId", id, "offerValidity", "P14D"))
            .send()
            .join();
    assertThat(instance).hasActiveElements("AssessDamage");
    assertThat(byElementId("AssessDamage", instance.getProcessInstanceKey()))
        .hasCandidateGroups(java.util.List.of("technicians"));
    context.completeUserTask(
        "AssessDamage",
        Map.of("damageDescription", "Bremse", "effortHours", 2, "materialCost", 10));
    context.completeJob("create-offer", Map.of("offerAmount", 160));
    assertThat(byElementId("ReviewOffer", instance.getProcessInstanceKey()))
        .hasCandidateGroups(java.util.List.of("customer-service"));
    context.completeUserTask("ReviewOffer", review);
    return instance;
  }

  private void waitForDecision(ProcessInstanceEvent instance) {
    assertThat(instance).hasActiveElements("SendOffer");
    context.completeJob("send-offer");
    assertThat(instance)
        .hasActiveElements("AwaitCustomerDecision")
        .isWaitingForMessage("BikeRepairOfferAccepted")
        .isWaitingForMessage("BikeRepairOfferRejected");
  }

  private void message(String name, String id) {
    client
        .newPublishMessageCommand()
        .messageName(name)
        .correlationKey(id)
        .timeToLive(Duration.ZERO)
        .send()
        .join();
  }
}
