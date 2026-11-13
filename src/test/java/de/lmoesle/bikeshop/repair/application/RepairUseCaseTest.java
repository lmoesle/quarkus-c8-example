package de.lmoesle.bikeshop.repair.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.application.ports.out.*;
import de.lmoesle.bikeshop.repair.application.usecases.RepairUseCase;
import de.lmoesle.bikeshop.repair.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RepairUseCaseTest {
  private final Workflow workflow = mock(Workflow.class);
  private final TaskProjection tasks = mock(TaskProjection.class);
  private final RepairUseCase useCase = new RepairUseCase(workflow, tasks, "P14D");

  @Test
  void generatesIdentityAndKeepsTimerApplicationControlled() {
    when(workflow.start(anyMap())).thenReturn("2251799813685249");
    final var result =
        useCase.start(
            new Repairs.Start(new RepairRequest("Ada", "ada@example.org", "Rad", "Bremse")));
    assertThat(UUID.fromString(result.repairId())).isNotNull();
    verify(workflow)
        .start(
            argThat(v ->
                v.get("offerValidity").equals("P14D")
                    && v.get("repairId").equals(result.repairId())));
    assertThat(result.processInstanceId()).isEqualTo("2251799813685249");
  }

  @Test
  void evictsOnlyAfterSuccessfulAcknowledgement() {
    when(tasks.find("1"))
        .thenReturn(
            Optional.of(
                new RepairTask("1", "2", "ReviewOffer", "3", "Prüfen", List.of(), Map.of())));
    final var command = new Repairs.Completion("1", Map.of("reviewDecision", "APPROVE"));
    doThrow(new IllegalStateException("engine unavailable"))
        .when(workflow)
        .complete(anyString(), anyMap());
    assertThatIllegalStateException().isThrownBy(() -> useCase.complete(command));
    verify(tasks, never()).delete(anyString());
    doNothing().when(workflow).complete(anyString(), anyMap());
    useCase.complete(command);
    final var order = inOrder(workflow, tasks);
    order.verify(workflow).complete(eq("1"), anyMap());
    order.verify(tasks).delete("1");
  }

  @Test
  void validatesDurationAndUnknownTasks() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RepairUseCase(workflow, tasks, "PT0S"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RepairUseCase(workflow, tasks, "-P1D"));
    when(tasks.find("missing")).thenReturn(Optional.empty());
    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> useCase.task("missing"));
  }
}
