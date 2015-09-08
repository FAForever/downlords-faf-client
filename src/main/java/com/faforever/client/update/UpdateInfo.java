package com.faforever.client.update;

import java.net.URL;

public class UpdateInfo {

  private String name;
  private String fileName;
  private URL url;
  private long size;
  private URL releaseNotesUrl;

  public UpdateInfo(String name, String fileName, URL url, long size, URL releaseNotesUrl) {
    this.name = name;
    this.fileName = fileName;
    this.url = url;
    this.size = size;
    this.releaseNotesUrl = releaseNotesUrl;
  }

  public String getName() {
    return name;
  }

  public URL getUrl() {
    return url;
  }

  public long getSize() {
    return size;
  }

  public URL getReleaseNotesUrl() {
    return releaseNotesUrl;
  }

  public String getFileName() {
    return fileName;
  }
}
