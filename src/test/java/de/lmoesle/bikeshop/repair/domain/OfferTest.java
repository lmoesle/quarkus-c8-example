package de.lmoesle.bikeshop.repair.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OfferTest {
  @Test
  void calculatesEurAndRoundsOnlyTotal() {
    assertThat(Offer.calculate(new BigDecimal("1.125"), new BigDecimal("10.001")).amount())
        .isEqualByComparingTo("94.38");
  }

  @Test
  void revisionUsesNewPriceAndRequiresReason() {
    final var original = new Offer(new BigDecimal("120"));
    assertThat(original.revise(new BigDecimal("99.995"), "Kulanz").amount())
        .isEqualByComparingTo("100.00");
    assertThat(original.amount()).isEqualByComparingTo("120.00");
    assertThatIllegalArgumentException().isThrownBy(() -> original.revise(BigDecimal.TEN, " "));
  }

  @Test
  void rejectsInvalidAndUnboundedAmounts() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Offer(null));
    assertThatIllegalArgumentException().isThrownBy(() -> new Offer(BigDecimal.ZERO));
    assertThatIllegalArgumentException().isThrownBy(() -> new Offer(new BigDecimal("0.001")));
    assertThatIllegalArgumentException().isThrownBy(() -> new Offer(new BigDecimal("100001")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Offer.calculate(BigDecimal.ZERO, BigDecimal.TEN));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Offer.calculate(BigDecimal.ONE, new BigDecimal("-1")));
  }
}
