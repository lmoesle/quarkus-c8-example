package de.lmoesle.bikeshop.repair.adapter.in.rest;

import de.lmoesle.bikeshop.repair.application.ports.in.Repairs;
import de.lmoesle.bikeshop.repair.domain.RepairRequest;
import de.lmoesle.bikeshop.repair.domain.RepairTask;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RepairResource {
  private final Repairs repairs;

  public RepairResource(Repairs repairs) {
    this.repairs = repairs;
  }

  @POST
  @Path("repairs")
  public Response start(RepairRequest request) {
    if (request == null) throw new IllegalArgumentException("request is required");
    final var started = repairs.start(new Repairs.Start(request));
    return Response.created(URI.create("/tasks?repairId=" + started.repairId()))
        .entity(started)
        .build();
  }

  @GET
  @Path("tasks")
  public List<RepairTask> tasks(
      @QueryParam("repairId") String repairId,
      @QueryParam("type") String type,
      @QueryParam("group") String group) {
    return repairs.tasks(repairId, type, group);
  }

  @GET
  @Path("tasks/{id}")
  public RepairTask task(@PathParam("id") String id) {
    return repairs.task(id);
  }

  @POST
  @Path("tasks/{id}/complete")
  public Response complete(@PathParam("id") String id, Map<String, Object> payload) {
    repairs.complete(new Repairs.Completion(id, payload));
    return Response.noContent().build();
  }

  @POST
  @Path("repairs/{id}/accept")
  public Response accept(@PathParam("id") String id) {
    return decision(id, true);
  }

  @POST
  @Path("repairs/{id}/reject")
  public Response reject(@PathParam("id") String id) {
    return decision(id, false);
  }

  private Response decision(String id, boolean accepted) {
    repairs.decide(new Repairs.Decision(id, accepted));
    return Response.accepted(Map.of("status", "published", "correlationConfirmed", false)).build();
  }
}
