package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Review {
  @Id
  private String id;
  private String text;
  private Byte score;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("player")
  private Player player;
}
