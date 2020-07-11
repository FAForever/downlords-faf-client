package com.faforever.client.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
public class GitHubRelease {
  private boolean prerelease;
  private String name;
  @JsonProperty("tag_name")
  private String tagName;
  @JsonProperty("html_url")
  private URL releaseNotes;
  private List<GitHubAsset> assets;
}
