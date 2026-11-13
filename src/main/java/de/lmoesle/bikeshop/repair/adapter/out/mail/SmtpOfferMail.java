package de.lmoesle.bikeshop.repair.adapter.out.mail;

import de.lmoesle.bikeshop.repair.application.ports.out.OfferMail;
import de.lmoesle.bikeshop.repair.domain.Offer;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SmtpOfferMail implements OfferMail {
  private final Mailer mailer;
  private final String publicUrl;

  public SmtpOfferMail(
      Mailer mailer, @ConfigProperty(name = "bike-repair.public-url") String publicUrl) {
    this.mailer = mailer;
    this.publicUrl = publicUrl;
  }

  public void send(String repairId, String name, String email, Offer offer) {
    final var body =
        """
        Guten Tag %s,
        Ihr Reparaturangebot %s beträgt %s EUR.
        Bitte antworten Sie innerhalb der Angebotsfrist (standardmäßig 14 Tage).
        Annahme: POST %s/repairs/%s/accept
        Ablehnung: POST %s/repairs/%s/reject
        Keine Aktion wird durch das Öffnen eines Links ausgelöst.
        """
            .formatted(
                name,
                repairId,
                offer.amount().toPlainString(),
                publicUrl,
                repairId,
                publicUrl,
                repairId);
    mailer.send(Mail.withText(email, "Reparaturangebot " + repairId, body));
  }
}
