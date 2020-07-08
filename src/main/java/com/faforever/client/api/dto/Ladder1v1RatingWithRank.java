package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

@Data
@Type("Ladder1v1RatingWithRank")
public class Ladder1v1RatingWithRank {
  @Id
  private String id;
  private double mean;
  private double deviation;
  private double rating;
  private int rank;

  @Relationship("player")
  private Player player;
}