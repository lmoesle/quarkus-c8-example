package de.lmoesle.bikeshop.repair.application.usecases;

import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.application.ports.out.Workflow;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import de.lmoesle.bikeshop.repair.domain.TaskCompletion;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class RepairUseCase implements Repairs {
  private final Workflow workflow;
  private final TaskProjection tasks;
  private final String validity;

  public RepairUseCase(Workflow workflow, TaskProjection tasks, String validity) {
    final var duration = Duration.parse(validity);
    if (duration.isNegative() || duration.isZero())
      throw new IllegalArgumentException("offerValidity must be positive");
    this.workflow = workflow;
    this.tasks = tasks;
    this.validity = validity;
  }

  public Started start(Start command) {
    final var id = UUID.randomUUID().toString();
    final var request = command.request();
    final var instance =
        workflow.start(
            Map.of(
                "repairId",
                id,
                "customerName",
                request.customerName(),
                "customerEmail",
                request.customerEmail(),
                "bikeDescription",
                request.bikeDescription(),
                "repairRequest",
                request.repairRequest(),
                "offerValidity",
                validity));
    return new Started(id, instance);
  }

  public void decide(Decision command) {
    workflow.publish(command.repairId(), command.accepted());
  }

  public void complete(Completion command) {
    final var task = task(command.taskId());
    final var variables = TaskCompletion.variables(task.elementId(), command.payload());
    workflow.complete(task.id(), variables);
    tasks.delete(
        task.id()); // Deliberately after engine acknowledgement, not a distributed transaction.
  }

  public RepairTask task(String id) {
    return tasks.find(id).orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
  }

  public List<RepairTask> tasks(String repairId, String type, String group) {
    return tasks.list(repairId, type, group);
  }
}
