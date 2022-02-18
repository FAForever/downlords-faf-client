package com.faforever.client.update;

import lombok.Value;

import java.net.URL;

@Value
public class UpdateInfo {
  String name;
  String fileName;
  URL url;
  int size;
  URL releaseNotesUrl;
  boolean prerelease;
}
