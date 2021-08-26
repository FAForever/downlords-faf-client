package com.faforever.client.remote.gson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;

public abstract class ComparableVersionMixin {

  @JsonCreator
  public ComparableVersion create(String value) {
    String version = value.startsWith("v") ? value.substring(1) : value;
    return new ComparableVersion(version);
  }

  @JsonValue
  public String read(ComparableVersion comparableVersion) throws IOException {
    return comparableVersion.toString();
  }
}
