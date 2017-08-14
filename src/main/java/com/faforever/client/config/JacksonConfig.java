package com.faforever.client.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

  @Bean
  public Module customDeserializers() {
    SimpleModule fafModule = new SimpleModule();
    fafModule.addDeserializer(ComparableVersion.class, new ComparableVersionDeserializer());
    return fafModule;
  }

  private static class ComparableVersionDeserializer extends JsonDeserializer<ComparableVersion> {

    @Override
    public ComparableVersion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return new ComparableVersion(p.getValueAsString());
    }
  }
}
