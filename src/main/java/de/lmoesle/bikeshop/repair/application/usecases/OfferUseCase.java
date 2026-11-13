package de.lmoesle.bikeshop.repair.application.usecases;

import de.lmoesle.bikeshop.repair.application.ports.in.Offers;
import de.lmoesle.bikeshop.repair.application.ports.out.OfferMail;
import de.lmoesle.bikeshop.repair.domain.Offer;

public final class OfferUseCase implements Offers {
  private final OfferMail mail;

  public OfferUseCase(OfferMail mail) {
    this.mail = mail;
  }

  public Offer create(Estimate command) {
    return Offer.calculate(command.effortHours(), command.materialCost());
  }

  public void send(Send command) {
    mail.send(command.repairId(), command.customerName(), command.customerEmail(), command.offer());
  }
}
