package com.faforever.client.domain.api;

import java.util.List;

public record TutorialCategory(Integer id, String category, String categoryKey, List<Tutorial> tutorials) {
  public TutorialCategory {
    tutorials = tutorials == null ? List.of() : List.copyOf(tutorials);
  }
}
