package com.faforever.client.update;

import lombok.Value;
import org.update4j.Configuration;

import java.net.URL;

@Value
public class UpdateInfo {

  String name;
  Configuration configuration;
  long size;
  URL releaseNotesUrl;
  boolean prerelease;

}
