package com.faforever.client.builders;

import com.faforever.client.domain.FeaturedModBean;


public class FeaturedModBeanBuilder {
  public static FeaturedModBeanBuilder create() {
    return new FeaturedModBeanBuilder();
  }

  private final FeaturedModBean featuredModBean = new FeaturedModBean();

  public FeaturedModBeanBuilder defaultValues() {
    technicalName("faf");
    visible(true);
    description("Standard mod");
    displayName("Forged Alliance Forever");
    gitUrl("http://localhost/example.git");
    id(0);
    return this;
  }

  public FeaturedModBeanBuilder id(Integer id) {
    featuredModBean.setId(id);
    return this;
  }

  public FeaturedModBeanBuilder technicalName(String technicalName) {
    featuredModBean.setTechnicalName(technicalName);
    return this;
  }

  public FeaturedModBeanBuilder displayName(String displayName) {
    featuredModBean.setDisplayName(displayName);
    return this;
  }

  public FeaturedModBeanBuilder description(String description) {
    featuredModBean.setDescription(description);
    return this;
  }

  public FeaturedModBeanBuilder gitUrl(String gitUrl) {
    featuredModBean.setGitUrl(gitUrl);
    return this;
  }

  public FeaturedModBeanBuilder gitBranch(String gitBranch) {
    featuredModBean.setGitBranch(gitBranch);
    return this;
  }

  public FeaturedModBeanBuilder visible(boolean visible) {
    featuredModBean.setVisible(visible);
    return this;
  }

  public FeaturedModBean get() {
    return featuredModBean;
  }

}

