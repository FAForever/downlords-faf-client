package com.faforever.client.domain;

import java.net.URL;
import java.util.List;

public record ClanBean(
    Integer id,
    String description,
    PlayerBean founder,
    PlayerBean leader,
    String name,
    String tag,
    String tagColor,
    URL websiteUrl,
    List<PlayerBean> members
) {

  public ClanBean {
    members = members == null ? List.of() : List.copyOf(members);
  }
}
