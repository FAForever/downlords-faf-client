package com.faforever.client.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;

@Data
public class GitHubAssets {
  private String name;
  @JsonProperty("browser_download_url")
  private URL browserDownloadUrl;
  private int size;
}
