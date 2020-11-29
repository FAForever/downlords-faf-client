package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Type("modReviewsSummary")
@Getter
@Setter
public class ModReviewsSummary extends ReviewsSummary {

  @Relationship("mod")
  private Mod mod;
}
