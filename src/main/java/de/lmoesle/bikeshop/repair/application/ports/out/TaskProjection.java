package de.lmoesle.bikeshop.repair.application.ports.out;

import de.lmoesle.bikeshop.repair.domain.RepairTask;
import java.util.List;
import java.util.Optional;

public interface TaskProjection {
  void reset();

  void upsert(RepairTask task);

  void delete(String id);

  Optional<RepairTask> find(String id);

  List<RepairTask> list(String repairId, String type, String group);
}
