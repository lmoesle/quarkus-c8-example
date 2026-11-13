package de.lmoesle.bikeshop.repair.adapter.out.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.JsonFormat;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

@Singleton
public class ProjectionJsonFormat {
  @Produces
  @Unremovable
  @Singleton
  @JsonFormat
  @PersistenceUnitExtension
  FormatMapper jsonFormatMapper() {
    // Explicit persistence format: independent of REST customizers and exact for untyped decimals.
    return new JacksonJsonFormatMapper(
        new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
  }
}
