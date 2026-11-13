package de.lmoesle.bikeshop.repair.adapter.out.engine;

import de.lmoesle.bikeshop.repair.application.ports.out.Workflow;
import dev.bpmcrafters.processengineapi.correlation.*;
import dev.bpmcrafters.processengineapi.process.*;
import dev.bpmcrafters.processengineapi.task.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class EngineWorkflow implements Workflow {
  private final StartProcessApi processes;
  private final CorrelationApi messages;
  private final UserTaskCompletionApi completion;

  public EngineWorkflow(
      StartProcessApi processes, CorrelationApi messages, UserTaskCompletionApi completion) {
    this.processes = processes;
    this.messages = messages;
    this.completion = completion;
  }

  public String start(Map<String, Object> variables) {
    return processes
        .startProcess(new StartProcessByDefinitionCmd("BikeRepair", () -> variables, Map.of()))
        .join()
        .getInstanceId();
  }

  public void publish(String repairId, boolean accepted) {
    final var name = accepted ? "BikeRepairOfferAccepted" : "BikeRepairOfferRejected";
    messages
        .correlateMessage(
            new CorrelateMessageCmd(name, Map::of, () -> Correlation.withKey(repairId), Map.of()))
        .join();
  }

  public void complete(String taskId, Map<String, Object> variables) {
    completion.completeTask(new CompleteTaskCmd(taskId, () -> variables)).join();
  }
}
