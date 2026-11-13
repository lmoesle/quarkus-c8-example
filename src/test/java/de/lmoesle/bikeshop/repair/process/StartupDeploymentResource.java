package de.lmoesle.bikeshop.repair.process;

import io.camunda.process.test.impl.containers.CamundaContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.time.Duration;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class StartupDeploymentResource implements QuarkusTestResourceLifecycleManager {
  private final CamundaContainer camunda =
      new CamundaContainer(DockerImageName.parse("camunda/camunda:8.8.46"))
          .withStartupTimeout(Duration.ofMinutes(3));
  private final GenericContainer<?> postgres =
      new GenericContainer<>("postgres:16-alpine")
          .withEnv("POSTGRES_USER", "bikerepair")
          .withEnv("POSTGRES_PASSWORD", "bikerepair")
          .withEnv("POSTGRES_DB", "bikerepair")
          .withExposedPorts(5432);

  @Override
  public Map<String, String> start() {
    try {
      camunda.start();
      postgres.start();
      return Map.of(
          "quarkus.http.test-port",
          "0",
          "quarkus.datasource.jdbc.url",
          "jdbc:postgresql://"
              + postgres.getHost()
              + ":"
              + postgres.getMappedPort(5432)
              + "/bikerepair",
          "quarkus.process-engine-api.c8.client.grpc-address",
          camunda.getGrpcApiAddress().toString(),
          "quarkus.process-engine-api.c8.client.rest-address",
          camunda.getRestApiAddress().toString(),
          "quarkus.mailer.mock",
          "true");
    } catch (RuntimeException failure) {
      stop();
      throw failure;
    }
  }

  @Override
  public void stop() {
    try {
      postgres.stop();
    } finally {
      camunda.stop();
    }
  }
}
