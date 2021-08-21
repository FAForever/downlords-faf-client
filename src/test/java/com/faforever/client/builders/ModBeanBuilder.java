package com.faforever.client.builders;

import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;
import java.util.List;


public class ModBeanBuilder {
  public static ModBeanBuilder create() {
    return new ModBeanBuilder();
  }

  private final ModBean modBean = new ModBean();

  public ModBeanBuilder defaultValues() {
    recommended(true);
    displayName("test mod");
    author("test author");
    uploader(PlayerBeanBuilder.create().defaultValues().get());
    versions(List.of());
    id(0);
    return this;
  }

  public ModBeanBuilder displayName(String displayName) {
    modBean.setDisplayName(displayName);
    return this;
  }

  public ModBeanBuilder recommended(boolean recommended) {
    modBean.setRecommended(recommended);
    return this;
  }

  public ModBeanBuilder author(String author) {
    modBean.setAuthor(author);
    return this;
  }

  public ModBeanBuilder uploader(PlayerBean uploader) {
    modBean.setUploader(uploader);
    return this;
  }

  public ModBeanBuilder modReviewsSummary(ModReviewsSummaryBean modReviewsSummary) {
    modBean.setModReviewsSummary(modReviewsSummary);
    return this;
  }

  public ModBeanBuilder versions(List<ModVersionBean> versions) {
    modBean.setVersions(versions);
    return this;
  }

  public ModBeanBuilder latestVersion(ModVersionBean latestVersion) {
    modBean.setLatestVersion(latestVersion);
    return this;
  }

  public ModBeanBuilder id(Integer id) {
    modBean.setId(id);
    return this;
  }

  public ModBeanBuilder createTime(OffsetDateTime createTime) {
    modBean.setCreateTime(createTime);
    return this;
  }

  public ModBeanBuilder updateTime(OffsetDateTime updateTime) {
    modBean.setUpdateTime(updateTime);
    return this;
  }

  public ModBean get() {
    return modBean;
  }

}

