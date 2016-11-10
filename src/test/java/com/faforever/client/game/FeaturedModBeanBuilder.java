package com.faforever.client.game;

public class FeaturedModBeanBuilder {

  private final FeaturedModBean featuredModBean;

  private FeaturedModBeanBuilder() {
    featuredModBean = new FeaturedModBean();
  }

  public static FeaturedModBeanBuilder create() {
    return new FeaturedModBeanBuilder();
  }

  public FeaturedModBeanBuilder defaultValues() {
    featuredModBean.setTechnicalName("faf");
    featuredModBean.setVisible(true);
    featuredModBean.setDescription("Standard mod");
    featuredModBean.setDisplayName("Forged Alliance Forever");
    featuredModBean.setGitUrl("http://localhost/example.git");
    return this;
  }

  public FeaturedModBean get() {
    return featuredModBean;
  }

  public FeaturedModBeanBuilder technicalName(String technicalName) {
    featuredModBean.setTechnicalName(technicalName);
    return this;
  }
}
