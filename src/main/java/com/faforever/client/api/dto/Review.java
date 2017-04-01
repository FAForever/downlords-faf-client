package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Review {
  @Id
  private String id;
  private String text;
  private Byte score;
  private Timestamp createTime;
  private Timestamp updateTime;

  @Relationship("player")
  private Player player;
}
