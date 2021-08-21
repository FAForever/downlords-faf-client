package com.faforever.client.builders;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.domain.TutorialCategoryBean;

import java.time.OffsetDateTime;


public class TutorialBeanBuilder {
  public static TutorialBeanBuilder create() {
    return new TutorialBeanBuilder();
  }

  private final TutorialBean tutorialBean = new TutorialBean();

  public TutorialBeanBuilder defaultValues() {
    title("test title");
    description("test desc");
    category(TutorialCategoryBeanBuilder.create().defaultValues().get());
    ordinal(0);
    mapVersion(MapVersionBeanBuilder.create().defaultValues().get());
    technicalName("tut1");
    id(0);
    return this;
  }

  public TutorialBeanBuilder title(String title) {
    tutorialBean.setTitle(title);
    return this;
  }

  public TutorialBeanBuilder description(String description) {
    tutorialBean.setDescription(description);
    return this;
  }

  public TutorialBeanBuilder category(TutorialCategoryBean category) {
    tutorialBean.setCategory(category);
    return this;
  }

  public TutorialBeanBuilder image(String image) {
    tutorialBean.setImage(image);
    return this;
  }

  public TutorialBeanBuilder imageUrl(String imageUrl) {
    tutorialBean.setImageUrl(imageUrl);
    return this;
  }

  public TutorialBeanBuilder ordinal(int ordinal) {
    tutorialBean.setOrdinal(ordinal);
    return this;
  }

  public TutorialBeanBuilder launchable(boolean launchable) {
    tutorialBean.setLaunchable(launchable);
    return this;
  }

  public TutorialBeanBuilder mapVersion(MapVersionBean mapVersion) {
    tutorialBean.setMapVersion(mapVersion);
    return this;
  }

  public TutorialBeanBuilder technicalName(String technicalName) {
    tutorialBean.setTechnicalName(technicalName);
    return this;
  }

  public TutorialBeanBuilder id(Integer id) {
    tutorialBean.setId(id);
    return this;
  }

  public TutorialBeanBuilder createTime(OffsetDateTime createTime) {
    tutorialBean.setCreateTime(createTime);
    return this;
  }

  public TutorialBeanBuilder updateTime(OffsetDateTime updateTime) {
    tutorialBean.setUpdateTime(updateTime);
    return this;
  }

  public TutorialBean get() {
    return tutorialBean;
  }

}

