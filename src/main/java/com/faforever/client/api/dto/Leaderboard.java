package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("leaderboard")
public class Leaderboard {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String description_key;
  private String name_key;
  private String technical_name;
}