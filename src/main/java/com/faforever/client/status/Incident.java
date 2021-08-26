package com.faforever.client.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * An information about a currently impacted service. See <a href="https://docs.statping.com/#folder-incidents">StatsPing
 * Incident</a>.
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Incident {
  @JsonProperty("id")
  long id;

  /** E.g. "Map download broken". */
  @JsonProperty("title")
  String title;
  /** E.g. "Users reported that maps can't be downloaded." */
  @JsonProperty("description")
  String description;
  @JsonProperty("service")
  long serviceId;
  @JsonProperty("created_at")
  OffsetDateTime createdAt;
  @JsonProperty("updated_at")
  OffsetDateTime updatedAt;
}
