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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Configuration
public class JacksonConfig {

  @Bean
  public Module customDeserializers() {
    SimpleModule fafModule = new SimpleModule();
    fafModule.addDeserializer(ComparableVersion.class, new ComparableVersionDeserializer());
    fafModule.addDeserializer(URL.class, new UrlDeserializer());
    return fafModule;
  }

  private static class ComparableVersionDeserializer extends JsonDeserializer<ComparableVersion> {

    @Override
    public ComparableVersion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return new ComparableVersion(p.getValueAsString());
    }
  }

  private static class UrlDeserializer extends JsonDeserializer<URL> {

    @Override
    public URL deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      // the default URL parsing is bad, see https://stackoverflow.com/questions/10786042/java-url-encoding-of-query-string-parameters
      URL url = new URL(p.getValueAsString());
      try {
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        return uri.toURL();
      } catch (URISyntaxException e) {
        throw new IOException("Could not parse URL!", e);
      }
    }
  }
}
