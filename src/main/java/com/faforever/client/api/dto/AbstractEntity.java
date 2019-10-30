package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public abstract class AbstractEntity {
  @Id
  protected String id;
  protected OffsetDateTime createTime;
  protected OffsetDateTime updateTime;
}