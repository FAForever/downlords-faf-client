package com.faforever.client.api;

import com.google.api.client.util.Key;

public class FeaturedModFile {
  @Key
  private String id;
  @Key
  private String version;
  @Key
  private String group;
  @Key
  private String name;
  @Key
  private String md5;
  @Key
  private String url;

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public String getGroup() {
    return group;
  }

  public String getName() {
    return name;
  }

  public String getMd5() {
    return md5;
  }

  public String getUrl() {
    return url;
  }
}
