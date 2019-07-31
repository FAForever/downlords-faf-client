package com.faforever.client.update;

import lombok.Value;

import java.net.URL;

@Value
public class UpdateInfo {

  private String name;
  private String fileName;
  private URL url;
  private int size;
  private URL releaseNotesUrl;

}
