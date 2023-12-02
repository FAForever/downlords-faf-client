package com.faforever.client.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * A message is an alert for scheduled downtime. See <a href="https://docs.statping.com/#folder-messages">StatPing
 * Message</a>.
 *
 * @param title E.g. "Server Maintenance"
 * @param description E.g. "Various services might be offline as we are installing a security patch.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
    @JsonProperty("id") long id,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("start_on") OffsetDateTime startOn,
    @JsonProperty("end_on") OffsetDateTime endOn,
    @JsonProperty("service") long serviceId,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
