package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Type("modVersionReview")
public class ModVersionReview extends Review {

  @Relationship("modVersion")
  private ModVersion modVersion;
}
