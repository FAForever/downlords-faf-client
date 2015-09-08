package com.faforever.client.update;

import java.net.URL;

class GitHubAsset {

  private URL browserDownloadUrl;
  private long size;
  private String name;

  public URL getBrowserDownloadUrl() {
    return browserDownloadUrl;
  }

  public long getSize() {
    return size;
  }

  public String getName() {
    return name;
  }
}
