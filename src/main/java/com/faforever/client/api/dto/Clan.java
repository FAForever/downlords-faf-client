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
  private String id;
  private String name;
  private String tag;
  private String description;
  private String tagColor;
  private String websiteUrl;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("founder")
  private Player founder;

  @Relationship("leader")
  private Player leader;

  @Relationship("memberships")
  private List<ClanMembership> memberships;
}
