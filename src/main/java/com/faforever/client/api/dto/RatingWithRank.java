package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class RatingWithRank {
  @Id
  private String id;
  private double mean;
  private double deviation;
  private double rating;
  private int rank;

  @Relationship("player")
  private Player player;
}

