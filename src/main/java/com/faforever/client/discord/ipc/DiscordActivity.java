package com.faforever.client.discord.ipc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A Rich Presence activity as accepted by Discord's {@code SET_ACTIVITY} command.
 */
@JsonInclude(Include.NON_NULL)
public record DiscordActivity(
    String state,
    String details,
    Timestamps timestamps,
    Assets assets,
    Party party,
    Secrets secrets
) {

  /** @param start Unix time in milliseconds */
  @JsonInclude(Include.NON_NULL)
  public record Timestamps(Long start) {}

  @JsonInclude(Include.NON_NULL)
  public record Assets(
      @JsonProperty("large_image") String largeImage,
      @JsonProperty("small_image") String smallImage
  ) {}

  /** @param size current and maximum party size */
  @JsonInclude(Include.NON_NULL)
  public record Party(String id, List<Integer> size) {}

  @JsonInclude(Include.NON_NULL)
  public record Secrets(String join, String spectate) {}
}
