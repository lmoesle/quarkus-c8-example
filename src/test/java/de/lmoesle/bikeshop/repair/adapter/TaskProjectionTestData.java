package de.lmoesle.bikeshop.repair.adapter;

import de.lmoesle.bikeshop.repair.domain.RepairTask;
import java.util.List;
import java.util.Map;

final class TaskProjectionTestData {
  private TaskProjectionTestData() {}

  static RepairTask task(String id, String type, String group) {
    return new RepairTask(
        id,
        "2251799813685249",
        type,
        "repair",
        "Aufgabe",
        List.of(group),
        Map.of("repairId", "repair"));
  }
}
