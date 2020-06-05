package com.faforever.client.update;

import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.update4j.Configuration;

import java.net.URL;
import java.util.Optional;

@Value
public class UpdateInfo {

  String name;
  ComparableVersion version;
  Optional<Configuration> currentConfiguration;
  Configuration newConfiguration;
  long size;
  URL releaseNotesUrl;
  boolean prerelease;

}
