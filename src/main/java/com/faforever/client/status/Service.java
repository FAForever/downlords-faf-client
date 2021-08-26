package com.faforever.client.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

/** A service that is monitored. See <a href="https://docs.statping.com/#folder-services">StatPing Service</a>. */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {

  @JsonProperty("id")
  long id;
  @JsonProperty("incidents")
  List<Incident> incidents;
  @JsonProperty("last_error")
  OffsetDateTime lastError;
  @JsonProperty("last_success")
  OffsetDateTime lastSuccess;
  @JsonProperty("messages")
  List<Message> messages;
  @JsonProperty("name")
  String name;
  @JsonProperty("online")
  boolean online;
  /** Used to build a URL like {@literal https://example.com/service/<permalink>}. */
  @JsonProperty("permalink")
  String permalink;
}
