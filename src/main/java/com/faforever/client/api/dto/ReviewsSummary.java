package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@EqualsAndHashCode(of = "id")
@Setter
public class ReviewsSummary {
  @Id
  private String id;
  private float positive;
  private float negative;
  private float score;
  private int reviews;
  private float lowerBound;

}
