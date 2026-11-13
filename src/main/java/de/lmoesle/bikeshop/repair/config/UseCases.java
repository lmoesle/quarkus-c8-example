package de.lmoesle.bikeshop.repair.config;

import de.lmoesle.bikeshop.repair.application.ports.in.Offers;
import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.application.ports.out.OfferMail;
import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import de.lmoesle.bikeshop.repair.application.ports.out.Workflow;
import de.lmoesle.bikeshop.repair.application.usecases.OfferUseCase;
import de.lmoesle.bikeshop.repair.application.usecases.RepairUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UseCases {
  @Produces
  @ApplicationScoped
  Offers offers(OfferMail mail) {
    return new OfferUseCase(mail);
  }

  @Produces
  @ApplicationScoped
  Repairs repairs(
      Workflow workflow,
      TaskProjection tasks,
      @ConfigProperty(name = "bike-repair.offer-validity", defaultValue = "P14D") String validity) {
    return new RepairUseCase(workflow, tasks, validity);
  }
}
