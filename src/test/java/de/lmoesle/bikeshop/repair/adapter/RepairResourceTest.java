package de.lmoesle.bikeshop.repair.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.lmoesle.bikeshop.repair.adapter.in.rest.Errors;
import de.lmoesle.bikeshop.repair.adapter.in.rest.RepairResource;
import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.domain.RepairRequest;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class RepairResourceTest {
  @Test
  void returnsCreatedWithStringIdsAndHonestMessagePublicationStatus() {
    final var repairs = mock(Repairs.class);
    when(repairs.start(any())).thenReturn(new Repairs.Started("repair", "2251799813685249"));
    final var resource = new RepairResource(repairs);
    try (final var response =
        resource.start(new RepairRequest("Ada", "ada@example.org", "Rad", "Bremse"))) {
      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().toString()).isEqualTo("/tasks?repairId=repair");
      assertThat(((Repairs.Started) response.getEntity()).processInstanceId())
          .isEqualTo("2251799813685249");
    }
    try (final var response = resource.accept("repair")) {
      assertThat(response.getStatus()).isEqualTo(202);
      assertThat(response.getEntity().toString()).contains("correlationConfirmed=false");
    }
    verify(repairs).decide(new Repairs.Decision("repair", true));
  }

  @Test
  void mapsInvalidMissingAndEngineFailures() {
    final var mapper = new Errors();
    try (final var invalid = mapper.toResponse(new IllegalArgumentException("invalid amount"));
        final var missing = mapper.toResponse(new NoSuchElementException("missing task"));
        final var engine = mapper.toResponse(new IllegalStateException("engine unavailable"))) {
      assertThat(invalid.getStatus()).isEqualTo(400);
      assertThat(missing.getStatus()).isEqualTo(404);
      assertThat(engine.getStatus()).isEqualTo(502);
    }
  }
}
