package de.lmoesle.bikeshop.repair.application.ports.in;

import de.lmoesle.bikeshop.repair.domain.RepairRequest;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import java.util.List;
import java.util.Map;

public interface Repairs {
  Started start(Start command);

  void decide(Decision command);

  void complete(Completion command);

  RepairTask task(String id);

  List<RepairTask> tasks(String repairId, String type, String group);

  record Start(RepairRequest request) {}

  record Started(String repairId, String processInstanceId) {}

  record Decision(String repairId, boolean accepted) {}

  record Completion(String taskId, Map<String, Object> payload) {}
}
