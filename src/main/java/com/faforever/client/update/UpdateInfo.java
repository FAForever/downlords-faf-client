package com.faforever.client.update;

import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.update4j.Configuration;

import java.net.URL;

@Value
public class UpdateInfo {

  String name;
  ComparableVersion version;
  Configuration configuration;
  long size;
  URL releaseNotesUrl;
  boolean prerelease;

}
