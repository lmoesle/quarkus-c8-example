package de.lmoesle.bikeshop.repair.adapter.in.worker;

import de.lmoesle.bikeshop.repair.application.ports.in.Offers;
import de.lmoesle.bikeshop.repair.domain.Offer;
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Map;

@ApplicationScoped
public class OfferWorkers {
  private final Offers offers;

  public OfferWorkers(Offers offers) {
    this.offers = offers;
  }

  @ProcessEngineWorker(topic = "create-offer")
  public Map<String, Object> create(
      @Variable(name = "effortHours") BigDecimal effort,
      @Variable(name = "materialCost") BigDecimal materials) {
    return Map.of("offerAmount", offers.create(new Offers.Estimate(effort, materials)).amount());
  }

  @ProcessEngineWorker(topic = "send-offer")
  public Map<String, Object> send(
      @Variable(name = "repairId") String repairId,
      @Variable(name = "customerName") String name,
      @Variable(name = "customerEmail") String email,
      @Variable(name = "offerAmount") BigDecimal amount) {
    offers.send(new Offers.Send(repairId, name, email, new Offer(amount)));
    return Map.of();
  }
}
