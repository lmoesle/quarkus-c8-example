package de.lmoesle.bikeshop.repair.adapter.in.subscription;

import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import dev.bpmcrafters.processengineapi.task.*;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TaskSubscriptions {
  private final TaskSubscriptionApi subscriptions;
  private final TaskProjection tasks;
  private TaskSubscription subscription;

  public TaskSubscriptions(TaskSubscriptionApi subscriptions, TaskProjection tasks) {
    this.subscriptions = subscriptions;
    this.tasks = tasks;
  }

  public void start(@Observes StartupEvent event) {
    tasks.reset(); // Single-instance cache: clear stale rows BEFORE the adapter begins polling at
    // priority 3000.
    final TaskTerminationHandler termination = task -> tasks.delete(task.getTaskId());
    subscription =
        subscriptions
            .subscribeForTask(
                new SubscribeForTaskCmd(
                    Map.of("processDefinitionKey", "BikeRepair"),
                    TaskType.USER,
                    null,
                    null,
                    this::upsert,
                    termination))
            .join();
  }

  public void upsert(TaskInformation task, Map<String, ?> payload) {
    final var groups = task.getMetaValueAsStringSet("candidateGroups");
    tasks.upsert(
        new RepairTask(
            task.getTaskId(),
            task.getMeta().get("processInstanceId"),
            task.getMeta().get("activityId"),
            String.valueOf(payload.get("repairId")),
            task.getMeta().getOrDefault("taskName", "Aufgabe"),
            groups == null ? List.of() : List.copyOf(groups),
            new HashMap<>(payload)));
  }

  public void stop(@Observes ShutdownEvent event) {
    if (subscription != null)
      subscriptions.unsubscribe(new UnsubscribeFromTaskCmd(subscription)).join();
  }
}
