package com.faforever.client.builders;

import com.faforever.client.coop.CoopCategory;
import com.faforever.client.domain.CoopMissionBean;


public class CoopMissionBeanBuilder {
  public static CoopMissionBeanBuilder create() {
    return new CoopMissionBeanBuilder();
  }

  private final CoopMissionBean coopMissionBean = new CoopMissionBean();

  public CoopMissionBeanBuilder defaultValues() {
    id(0);
    return this;
  }

  public CoopMissionBeanBuilder id(Integer id) {
    coopMissionBean.setId(id);
    return this;
  }

  public CoopMissionBeanBuilder name(String name) {
    coopMissionBean.setName(name);
    return this;
  }

  public CoopMissionBeanBuilder description(String description) {
    coopMissionBean.setDescription(description);
    return this;
  }

  public CoopMissionBeanBuilder version(int version) {
    coopMissionBean.setVersion(version);
    return this;
  }

  public CoopMissionBeanBuilder category(CoopCategory category) {
    coopMissionBean.setCategory(category);
    return this;
  }

  public CoopMissionBeanBuilder downloadUrl(String downloadUrl) {
    coopMissionBean.setDownloadUrl(downloadUrl);
    return this;
  }

  public CoopMissionBeanBuilder thumbnailUrlSmall(String thumbnailUrlSmall) {
    coopMissionBean.setThumbnailUrlSmall(thumbnailUrlSmall);
    return this;
  }

  public CoopMissionBeanBuilder thumbnailUrlLarge(String thumbnailUrlLarge) {
    coopMissionBean.setThumbnailUrlLarge(thumbnailUrlLarge);
    return this;
  }

  public CoopMissionBeanBuilder mapFolderName(String mapFolderName) {
    coopMissionBean.setMapFolderName(mapFolderName);
    return this;
  }

  public CoopMissionBean get() {
    return coopMissionBean;
  }

}

