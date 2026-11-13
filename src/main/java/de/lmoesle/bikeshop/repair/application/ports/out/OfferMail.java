package de.lmoesle.bikeshop.repair.application.ports.out;

import de.lmoesle.bikeshop.repair.domain.Offer;

public interface OfferMail {
  void send(String repairId, String customerName, String email, Offer offer);
}
