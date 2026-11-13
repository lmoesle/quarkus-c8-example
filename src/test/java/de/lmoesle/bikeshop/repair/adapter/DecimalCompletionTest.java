package de.lmoesle.bikeshop.repair.adapter;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.lmoesle.bikeshop.repair.adapter.in.rest.RepairResource;
import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.application.ports.out.Workflow;
import de.lmoesle.bikeshop.repair.application.usecases.RepairUseCase;
import de.lmoesle.bikeshop.repair.config.DecimalJsonCustomizer;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DecimalCompletionTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Workflow workflow = mock(Workflow.class);
  private final TaskProjection tasks = mock(TaskProjection.class);
  private final RepairResource resource =
      new RepairResource(new RepairUseCase(workflow, tasks, "P14D"));

  DecimalCompletionTest() {
    new DecimalJsonCustomizer().customize(mapper);
  }

  @ParameterizedTest
  @CsvSource({"99.994999999999999, 99.99", "99.995, 100.00"})
  void roundsLiteralJsonExactlyAtCompletionBoundary(String literal, String expected)
      throws Exception {
    prepareRevision();
    final var payload = read("{\"offerAmount\":" + literal + ",\"revisionReason\":\"Kulanz\"}");
    try (final var response = resource.complete("task", payload)) {
      org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(204);
    }
    verify(workflow)
        .complete(
            eq("task"),
            argThat(variables -> new BigDecimal(expected).equals(variables.get("offerAmount"))));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"offerAmount\":null,\"revisionReason\":\"Kulanz\"}",
        "{\"offerAmount\":\"99.50\",\"revisionReason\":\"Kulanz\"}",
        "{\"offerAmount\":true,\"revisionReason\":\"Kulanz\"}",
        "{\"offerAmount\":[],\"revisionReason\":\"Kulanz\"}",
        "{\"offerAmount\":99.50,\"revisionReason\":null}"
      })
  void rejectsMissingNullAndWrongJsonTypesWithoutCompleting(String literal) throws Exception {
    prepareRevision();
    final var payload = read(literal);
    assertThatIllegalArgumentException().isThrownBy(() -> resource.complete("task", payload));
    verifyNoInteractions(workflow);
    verify(tasks, never()).delete(anyString());
  }

  private Map<String, Object> read(String literal) throws Exception {
    return mapper.readValue(literal, new TypeReference<Map<String, Object>>() {});
  }

  private void prepareRevision() {
    when(tasks.find("task"))
        .thenReturn(
            Optional.of(
                new RepairTask(
                    "task",
                    "instance",
                    "ReviseOffer",
                    "repair",
                    "Überarbeiten",
                    List.of(),
                    Map.of())));
  }
}
