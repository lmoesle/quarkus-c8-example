package de.lmoesle.bikeshop.repair.application.ports.out;

import java.util.Map;

public interface Workflow {
  String start(Map<String, Object> variables);

  void publish(String repairId, boolean accepted);

  void complete(String taskId, Map<String, Object> variables);
}
