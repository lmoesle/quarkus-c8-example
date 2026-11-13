package de.lmoesle.bikeshop.repair.adapter.in.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

@Provider
public class Errors implements ExceptionMapper<RuntimeException> {
  public Response toResponse(RuntimeException failure) {
    final var cause =
        failure instanceof CompletionException && failure.getCause() != null
            ? failure.getCause()
            : failure;
    final int status =
        switch (cause) {
          case IllegalArgumentException ignored -> 400;
          case NoSuchElementException ignored -> 404;
          case jakarta.ws.rs.WebApplicationException web -> web.getResponse().getStatus();
          default -> 502;
        };
    return Response.status(status)
        .entity(
            Map.of(
                "error",
                cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()))
        .build();
  }
}
