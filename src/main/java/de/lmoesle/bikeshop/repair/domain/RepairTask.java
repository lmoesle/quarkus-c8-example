package de.lmoesle.bikeshop.repair.domain;

import java.util.List;
import java.util.Map;

public record RepairTask(
    String id,
    String processInstanceId,
    String elementId,
    String repairId,
    String name,
    List<String> candidateGroups,
    Map<String, Object> payload) {
  public RepairTask {
    candidateGroups = List.copyOf(candidateGroups);
    payload = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(payload));
  }
}
