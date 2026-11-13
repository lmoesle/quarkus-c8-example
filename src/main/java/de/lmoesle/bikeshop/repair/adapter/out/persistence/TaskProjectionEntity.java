package de.lmoesle.bikeshop.repair.adapter.out.persistence;

import de.lmoesle.bikeshop.repair.domain.RepairTask;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "task_projection")
class TaskProjectionEntity {
  @Id
  @Column(name = "task_id", columnDefinition = "text")
  String id;

  @Column(name = "process_instance_id", nullable = false, columnDefinition = "text")
  String processInstanceId;

  @Column(name = "element_id", nullable = false, columnDefinition = "text")
  String elementId;

  @Column(name = "repair_id", nullable = false, columnDefinition = "text")
  String repairId;

  @Column(name = "display_name", nullable = false, columnDefinition = "text")
  String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "candidate_groups", nullable = false, columnDefinition = "jsonb")
  List<String> candidateGroups;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  Map<String, Object> payload;

  protected TaskProjectionEntity() {}

  TaskProjectionEntity(RepairTask task) {
    id = task.id();
    processInstanceId = task.processInstanceId();
    elementId = task.elementId();
    repairId = task.repairId();
    name = task.name();
    candidateGroups = task.candidateGroups();
    payload = task.payload();
  }

  RepairTask toTask() {
    return new RepairTask(
        id, processInstanceId, elementId, repairId, name, candidateGroups, payload);
  }
}
