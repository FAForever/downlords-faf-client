package com.faforever.client.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
public class GitHubRelease {
  private boolean prerelease;
  @JsonProperty("tag_name")
  private String tagName;
  @JsonProperty("html_url")
  private URL releaseNotes;
  private List<GitHubAssets> assets;
}
