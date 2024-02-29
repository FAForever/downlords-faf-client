package com.faforever.client.domain;

import java.util.List;

public record TutorialCategoryBean(Integer id, String category, String categoryKey, List<TutorialBean> tutorials) {
  public TutorialCategoryBean {
    tutorials = tutorials == null ? List.of() : List.copyOf(tutorials);
  }
}
