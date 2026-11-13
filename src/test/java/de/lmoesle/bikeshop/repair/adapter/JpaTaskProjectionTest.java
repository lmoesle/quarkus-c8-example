package de.lmoesle.bikeshop.repair.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import de.lmoesle.bikeshop.repair.process.StartupDeploymentResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = StartupDeploymentResource.class, restrictToAnnotatedClass = true)
class JpaTaskProjectionTest {
  @Inject TaskProjection projection;

  @BeforeEach
  void clearProjection() {
    projection.reset();
  }

  @Test
  void repeatedUpsertOverwritesEveryFieldAndFindsMissingIds() {
    final var original = TaskProjectionTestData.task("task", "AssessDamage", "technicians");
    final var changed =
        new RepairTask(
            "task",
            "new-process",
            "ReviewOffer",
            "other-repair",
            "Neue Aufgabe",
            List.of("customer-service", "supervisors"),
            Map.of("changed", true));
    assertThat(projection.find("missing")).isEmpty();
    projection.upsert(original);
    projection.upsert(original);
    assertThat(projection.list(null, null, null)).containsExactly(original);
    projection.upsert(changed);
    assertThat(projection.find("task")).contains(changed);
    assertThat(projection.list(null, null, null)).containsExactly(changed);
  }

  @Test
  void filtersIndependentlyAndTogetherWithExactGroupMembershipAndTextIdOrder() {
    final var first = TaskProjectionTestData.task("10", "AssessDamage", "technicians");
    final var second =
        new RepairTask(
            "2",
            "process",
            "ReviewOffer",
            "repair",
            "Review",
            List.of("technicians", "customer-service"),
            Map.of());
    final var third =
        new RepairTask("3", "process", "AssessDamage", "other", "Assess", List.of(), Map.of());
    projection.upsert(third);
    projection.upsert(second);
    projection.upsert(first);
    assertThat(projection.list(null, null, null)).containsExactly(first, second, third);
    assertThat(projection.list("repair", null, null)).containsExactly(first, second);
    assertThat(projection.list(null, "AssessDamage", null)).containsExactly(first, third);
    assertThat(projection.list(null, null, "technicians")).containsExactly(first, second);
    assertThat(projection.list(null, null, "customer-service")).containsExactly(second);
    assertThat(projection.list("repair", "ReviewOffer", "technicians")).containsExactly(second);
    assertThat(projection.list("other", "AssessDamage", "technicians")).isEmpty();
    assertThat(projection.list("missing", null, null)).isEmpty();
    assertThat(projection.list(null, "missing", null)).isEmpty();
    assertThat(projection.list(null, null, "tech")).isEmpty();
    assertThat(projection.list(null, null, "")).isEmpty();
  }

  @Test
  void repeatedIdOnlyDeleteAndResetAllowRepopulation() {
    final var first = TaskProjectionTestData.task("first", "AssessDamage", "technicians");
    final var second = TaskProjectionTestData.task("second", "AssessDamage", "technicians");
    projection.upsert(first);
    projection.upsert(second);
    projection.delete("first");
    projection.delete("first");
    projection.delete("missing");
    assertThat(projection.find("first")).isEmpty();
    assertThat(projection.list(null, null, null)).containsExactly(second);
    projection.reset();
    projection.reset();
    assertThat(projection.list(null, null, null)).isEmpty();
    projection.upsert(first);
    assertThat(projection.find("first")).contains(first);
  }

  @Test
  void roundTripsNestedAndNullPayloadValuesAndExactDecimals() {
    final var amount = new BigDecimal("12345.678901234567890123456789");
    final var nested = new HashMap<String, Object>();
    nested.put("amount", amount);
    nested.put("missing", null);
    final var payload = new HashMap<String, Object>();
    payload.put("nested", nested);
    payload.put("items", java.util.Arrays.asList(amount, null, Map.of("valid", true)));
    payload.put("missing", null);
    final var task =
        new RepairTask("json", "process", "AssessDamage", "repair", "JSON", List.of(), payload);
    projection.upsert(task);
    assertThat(projection.find("json")).contains(task);
    assertThat(projection.list(null, null, null)).containsExactly(task);
  }

  @Test
  void concurrentInitialUpsertsCommitOneCompleteSnapshot() throws Exception {
    final var snapshots =
        java.util.stream.IntStream.range(0, 12)
            .mapToObj(index ->
                new RepairTask(
                    "concurrent",
                    "process-" + index,
                    "type-" + index,
                    "repair-" + index,
                    "Name " + index,
                    List.of("group-" + index),
                    Map.of("index", index)))
            .toList();
    final var calls =
        snapshots.stream()
            .<Callable<Void>>map(task ->
                () -> {
                  projection.upsert(task);
                  return null;
                })
            .toList();
    runConcurrently(calls);
    assertThat(projection.find("concurrent")).isPresent();
    assertThat(snapshots).contains(projection.find("concurrent").orElseThrow());
    assertThat(projection.list(null, null, null)).hasSize(1);
  }

  @Test
  void concurrentUpsertAndDeleteRemainIdempotent() throws Exception {
    final var task = TaskProjectionTestData.task("race", "AssessDamage", "technicians");
    for (final var ignored : List.of(1, 2, 3, 4)) {
      runConcurrently(
          List.of(
              () -> {
                projection.upsert(task);
                return null;
              },
              () -> {
                projection.delete(task.id());
                return null;
              }));
      assertThat(projection.find(task.id()).stream())
          .allSatisfy(row -> assertThat(row).isEqualTo(task));
    }
    projection.upsert(task);
    assertThat(projection.find(task.id())).contains(task);
    projection.delete(task.id());
    assertThat(projection.find(task.id())).isEmpty();
  }

  @Test
  void ordinaryBackgroundThreadNeedsNoRequestContext() throws Exception {
    final var task = TaskProjectionTestData.task("background", "AssessDamage", "technicians");
    try (final var executor = Executors.newSingleThreadExecutor()) {
      executor
          .submit(() -> {
            projection.upsert(task);
            assertThat(projection.find(task.id())).contains(task);
            assertThat(projection.list("repair", "AssessDamage", "technicians"))
                .containsExactly(task);
            projection.delete(task.id());
            projection.reset();
          })
          .get(20, TimeUnit.SECONDS);
    }
    assertThat(projection.find(task.id())).isEmpty();
  }

  private void runConcurrently(List<Callable<Void>> calls) throws Exception {
    final var ready = new CountDownLatch(calls.size());
    final var start = new CountDownLatch(1);
    final var executor = Executors.newFixedThreadPool(calls.size());
    try {
      final var futures = new ArrayList<java.util.concurrent.Future<Void>>();
      for (final var call : calls) {
        futures.add(
            executor.submit(() -> {
              ready.countDown();
              if (!start.await(10, TimeUnit.SECONDS))
                throw new IllegalStateException("Start timed out");
              return call.call();
            }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (final var future : futures) future.get(20, TimeUnit.SECONDS);
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }
}
