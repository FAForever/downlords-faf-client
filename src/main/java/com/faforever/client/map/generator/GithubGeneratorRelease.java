package com.faforever.client.map.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GithubGeneratorRelease {
  @JsonProperty("tag_name")
  private String tagName;
  private String name;
  @JsonProperty("prerelease")
  private String preRelease;
  private String url;
}
