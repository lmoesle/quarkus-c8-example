package de.lmoesle.bikeshop.repair.domain;

public record RepairRequest(
    String customerName, String customerEmail, String bikeDescription, String repairRequest) {
  public RepairRequest {
    customerName = Checks.text(customerName, "customerName");
    customerEmail = Checks.text(customerEmail, "customerEmail");
    if (!customerEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new IllegalArgumentException("customerEmail must be an email address");
    }
    bikeDescription = Checks.text(bikeDescription, "bikeDescription");
    repairRequest = Checks.text(repairRequest, "repairRequest");
  }
}
