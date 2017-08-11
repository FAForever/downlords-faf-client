package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Type("gameReviewsSummary")
public class GameReviewsSummary extends ReviewsSummary {

  @Relationship("game")
  private Game game;
}
