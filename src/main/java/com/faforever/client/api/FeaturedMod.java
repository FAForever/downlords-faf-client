package com.faforever.client.api;

import com.google.api.client.util.Key;

public class FeaturedMod {

  @Key
  private String description;
  @Key("display_name")
  private String displayName;
  @Key("display_order")
  private int displayOrder;
  @Key("git_branch")
  private String gitBranch;
  @Key("git_url")
  private String gitUrl;
  @Key
  private String id;
  @Key("technical_name")
  private String technicalName;
  @Key
  private boolean visible;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public String getGitBranch() {
    return gitBranch;
  }

  public void setGitBranch(String gitBranch) {
    this.gitBranch = gitBranch;
  }

  public String getGitUrl() {
    return gitUrl;
  }

  public void setGitUrl(String gitUrl) {
    this.gitUrl = gitUrl;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTechnicalName() {
    return technicalName;
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName = technicalName;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}
