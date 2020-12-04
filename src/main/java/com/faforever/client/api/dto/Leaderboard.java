package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
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