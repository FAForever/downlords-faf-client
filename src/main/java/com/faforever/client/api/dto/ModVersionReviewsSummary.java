package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Type("modVersionReviewsSummary")
@Getter
@Setter
public class ModVersionReviewsSummary extends ReviewsSummary {

  @Relationship("modVersion")
  private ModVersion modVersion;
}
