package com.faforever.client.builders;

import com.faforever.client.domain.TutorialBean;
import com.faforever.client.domain.TutorialCategoryBean;

import java.util.List;


public class TutorialCategoryBeanBuilder {
  public static TutorialCategoryBeanBuilder create() {
    return new TutorialCategoryBeanBuilder();
  }

  private final TutorialCategoryBean tutorialCategoryBean = new TutorialCategoryBean();

  public TutorialCategoryBeanBuilder defaultValues() {
    tutorials(List.of());
    id(0);
    return this;
  }

  public TutorialCategoryBeanBuilder id(Integer id) {
    tutorialCategoryBean.setId(id);
    return this;
  }

  public TutorialCategoryBeanBuilder categoryKey(String categoryKey) {
    tutorialCategoryBean.setCategoryKey(categoryKey);
    return this;
  }

  public TutorialCategoryBeanBuilder category(String category) {
    tutorialCategoryBean.setCategory(category);
    return this;
  }

  public TutorialCategoryBeanBuilder tutorials(List<TutorialBean> tutorials) {
    tutorialCategoryBean.setTutorials(tutorials);
    return this;
  }

  public TutorialCategoryBean get() {
    return tutorialCategoryBean;
  }

}

