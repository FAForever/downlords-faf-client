package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Type("clan")
public class Clan {
  @Id
  private int id;
  private String name;
  private String tag;
  private Player founder;
  private Player leader;
  private String description;
  private String tagColor;
  private String websiteUrl;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("memberships")
  private List<ClanMembership> memberships;
}
