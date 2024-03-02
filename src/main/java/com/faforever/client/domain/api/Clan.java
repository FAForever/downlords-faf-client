package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;

import java.net.URL;
import java.util.List;

public record Clan(
    Integer id,
    String description, PlayerInfo founder, PlayerInfo leader,
    String name,
    String tag,
    String tagColor,
    URL websiteUrl, List<PlayerInfo> members
) {

  public Clan {
    members = members == null ? List.of() : List.copyOf(members);
  }
}
