package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Type("clanMembership")
public class ClanMembership {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("clan")
  private Clan clan;

  @Relationship("player")
  private Player player;
}
