package com.faforever.client.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * An information about a currently impacted service. See <a
 * href="https://docs.statping.com/#folder-incidents">StatsPing Incident</a>.
 *
 * @param title E.g. "Map download broken".
 * @param description E.g. "Users reported that maps can't be downloaded."
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Incident(
    @JsonProperty("id") long id,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("service") long serviceId,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
