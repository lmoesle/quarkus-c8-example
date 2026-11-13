package de.lmoesle.bikeshop.repair.process;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.application.ports.out.Workflow;
import de.lmoesle.bikeshop.repair.application.usecases.RepairUseCase;
import dev.bpmcrafters.processengine.worker.quarkus.deploymentresource.DeploymentResources;
import io.camunda.client.CamundaClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = StartupDeploymentResource.class, restrictToAnnotatedClass = true)
class StartupDeploymentTest {
  @Inject CamundaClient client;
  @Inject DeploymentResources resources;

  @Test
  void startupDeploysProcessAndAllDeploymentBoundFormsWithoutManualDeployment() {
    final var started =
        given()
            .contentType("application/json")
            .body(
                Map.of(
                    "customerName",
                    "Ada",
                    "customerEmail",
                    "ada@example.org",
                    "bikeDescription",
                    "Rad",
                    "repairRequest",
                    "Bremse"))
            .when()
            .post("/repairs")
            .then()
            .statusCode(201)
            .extract()
            .as(Repairs.Started.class);

    // All four BPMN form bindings use "deployment": resolution proves the shared startup bundle.
    final var assess = taskWithForm(started.repairId(), "AssessDamage", "assess-damage");
    complete(assess, Map.of("damageDescription", "Bremse", "effortHours", 2, "materialCost", 10));
    final var review = taskWithForm(started.repairId(), "ReviewOffer", "review-offer");
    complete(review, Map.of("reviewDecision", "REVISE"));
    final var revise = taskWithForm(started.repairId(), "ReviseOffer", "revise-offer");
    complete(revise, Map.of("offerAmount", 99.50, "revisionReason", "Kulanz"));
    given()
        .contentType("application/json")
        .post("/repairs/{id}/accept", started.repairId())
        .then()
        .statusCode(202);
    final var repair = taskWithForm(started.repairId(), "RepairBicycle", "repair-bicycle");
    complete(repair, Map.of("repairNotes", "Repariert"));
  }

  @Test
  void deploymentIsInfrastructureOnly() {
    given().post("/deployments").then().statusCode(404);
    for (final var type : java.util.List.of(Repairs.class, Workflow.class, RepairUseCase.class)) {
      assertThat(Arrays.stream(type.getDeclaredMethods()).map(java.lang.reflect.Method::getName))
          .doesNotContain("deploy");
    }
    assertThat(resources.paths().stream().map(path -> path.substring(path.lastIndexOf('/') + 1)))
        .containsExactlyInAnyOrder(
            "bike-repair.bpmn",
            "assess-damage.form",
            "review-offer.form",
            "revise-offer.form",
            "repair-bicycle.form");
  }

  private String taskWithForm(String repairId, String elementId, String formId) {
    final var taskId = new java.util.concurrent.atomic.AtomicReference<String>();
    await()
        .atMost(Duration.ofSeconds(60))
        .untilAsserted(() -> {
          final var tasks =
              given()
                  .queryParam("repairId", repairId)
                  .queryParam("type", elementId)
                  .get("/tasks")
                  .then()
                  .statusCode(200)
                  .extract()
                  .jsonPath()
                  .getList("id", String.class);
          assertThat(tasks).hasSize(1);
          final var form =
              client.newUserTaskGetFormRequest(Long.parseLong(tasks.getFirst())).send().join();
          assertThat(form.getFormId()).isEqualTo(formId);
          assertThat(form.getFormKey()).isPositive();
          assertThat(form.getSchema()).isNotNull();
          taskId.set(tasks.getFirst());
        });
    return taskId.get();
  }

  private void complete(String taskId, Map<String, Object> payload) {
    given()
        .contentType("application/json")
        .body(payload)
        .post("/tasks/{id}/complete", taskId)
        .then()
        .statusCode(204);
  }
}
