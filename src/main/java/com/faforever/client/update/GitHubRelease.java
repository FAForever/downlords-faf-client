package com.faforever.client.update;

import java.net.URL;
import java.util.Date;

class GitHubRelease {

  private String name;
  private Date publishedAt;
  private GitHubAsset[] assets;
  private URL htmlUrl;

  public String getName() {
    return name;
  }

  public Date getPublishedAt() {
    return publishedAt;
  }

  public GitHubAsset[] getAssets() {
    return assets;
  }

  public URL getHtmlUrl() {
    return htmlUrl;
  }
}
