package com.faforever.client.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
public class GitHubRelease {
  @JsonProperty("prerelease")
  private boolean preRelease;
  private String name;
  @JsonProperty("tag_name")
  private String tagName;
  @JsonProperty("html_url")
  private URL releaseNotes;
  private List<GitHubAsset> assets;
}
