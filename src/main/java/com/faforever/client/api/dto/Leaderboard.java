package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  //TODO: Remove JsonProperty if api gets rid of snake_case
  @JsonProperty("description_key")
  private String descriptionKey;
  @JsonProperty("name_key")
  private String nameKey;
  @JsonProperty("technical_name")
  private String technicalName;
}