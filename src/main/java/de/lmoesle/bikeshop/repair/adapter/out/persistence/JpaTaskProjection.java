package de.lmoesle.bikeshop.repair.adapter.out.persistence;

import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaTaskProjection implements TaskProjection {
  private final EntityManager entities;
  private final Object mutations = new Object();

  public JpaTaskProjection(EntityManager entities) {
    this.entities = entities;
  }

  public void reset() {
    mutate(() -> {
      final var delete =
          entities.getCriteriaBuilder().createCriteriaDelete(TaskProjectionEntity.class);
      delete.from(TaskProjectionEntity.class);
      entities.createQuery(delete).executeUpdate();
    });
  }

  public void delete(String id) {
    mutate(() -> {
      final var builder = entities.getCriteriaBuilder();
      final var delete = builder.createCriteriaDelete(TaskProjectionEntity.class);
      final var task = delete.from(TaskProjectionEntity.class);
      delete.where(builder.equal(task.get("id"), id));
      entities.createQuery(delete).executeUpdate();
    });
  }

  public void upsert(RepairTask task) {
    mutate(() -> entities.merge(new TaskProjectionEntity(task)));
  }

  public Optional<RepairTask> find(String id) {
    return QuarkusTransaction.requiringNew()
        .call(() ->
            Optional.ofNullable(entities.find(TaskProjectionEntity.class, id))
                .map(TaskProjectionEntity::toTask));
  }

  public List<RepairTask> list(String repairId, String type, String group) {
    return QuarkusTransaction.requiringNew()
        .call(() -> {
          final var builder = entities.getCriteriaBuilder();
          final var query = builder.createQuery(TaskProjectionEntity.class);
          final var task = query.from(TaskProjectionEntity.class);
          final var filters = new ArrayList<Predicate>();
          if (repairId != null) filters.add(builder.equal(task.get("repairId"), repairId));
          if (type != null) filters.add(builder.equal(task.get("elementId"), type));
          query
              .select(task)
              .where(filters.toArray(Predicate[]::new))
              .orderBy(builder.asc(task.get("id")));
          // This small, single-instance cache filters JSON array membership after the database
          // filters.
          return entities.createQuery(query).getResultList().stream()
              .map(TaskProjectionEntity::toTask)
              .filter(row -> group == null || row.candidateGroups().contains(group))
              .toList();
        });
  }

  private void mutate(Runnable mutation) {
    // Serialize through COMMIT, not just the method body of an @Transactional interceptor.
    // This cache supports one application instance; merge alone cannot atomically insert-if-absent.
    synchronized (mutations) {
      QuarkusTransaction.requiringNew().run(mutation);
    }
  }
}
