package de.lmoesle.bikeshop.repair.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.lmoesle.bikeshop.repair.adapter.in.worker.OfferWorkers;
import de.lmoesle.bikeshop.repair.application.ports.out.OfferMail;
import de.lmoesle.bikeshop.repair.application.usecases.OfferUseCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OfferWorkersTest {
  @Test
  void createsOfferAndMailsCurrentRatherThanOriginalAmount() {
    final var mail = mock(OfferMail.class);
    final var worker = new OfferWorkers(new OfferUseCase(mail));
    assertThat(worker.create(new BigDecimal("2"), BigDecimal.TEN))
        .containsEntry("offerAmount", new BigDecimal("160.00"));
    worker.send("repair", "Ada", "ada@example.org", new BigDecimal("99.50"));
    verify(mail)
        .send(
            eq("repair"),
            eq("Ada"),
            eq("ada@example.org"),
            argThat(o -> o.amount().equals(new BigDecimal("99.50"))));
  }
}
