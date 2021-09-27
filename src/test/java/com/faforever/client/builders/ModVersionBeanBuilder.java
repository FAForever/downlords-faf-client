package com.faforever.client.builders;

import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ModVersionReviewsSummaryBean;
import com.faforever.commons.mod.MountInfo;
import javafx.collections.FXCollections;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;


public class ModVersionBeanBuilder {
  public static ModVersionBeanBuilder create() {
    return new ModVersionBeanBuilder();
  }

  private final ModVersionBean modVersionBean = new ModVersionBean();

  public ModVersionBeanBuilder defaultValues() {
    mod(ModBeanBuilder.create().defaultValues().latestVersion(modVersionBean).get());
    imagePath(Path.of("."));
    id(0);
    uid("id");
    description("This is a test mod");
    selectable(true);
    version(new ComparableVersion("1"));
    try {
      thumbnailUrl(new URL("https://www.google.com"));
    } catch (MalformedURLException ignored) { }
    comments(FXCollections.observableArrayList());
    selected(false);
    played(100);
    try {
      downloadUrl(new URL("https://www.google.com"));
    } catch (MalformedURLException ignored) { }
    hookDirectories(FXCollections.observableArrayList());
    modType(ModType.UI);
    filename("foo.tmp");
    icon("testIcon");
    ranked(true);
    hidden(false);
    return this;
  }

  public ModVersionBeanBuilder imagePath(Path imagePath) {
    modVersionBean.setImagePath(imagePath);
    return this;
  }

  public ModVersionBeanBuilder uid(String uid) {
    modVersionBean.setUid(uid);
    return this;
  }

  public ModVersionBeanBuilder description(String description) {
    modVersionBean.setDescription(description);
    return this;
  }

  public ModVersionBeanBuilder selectable(boolean selectable) {
    modVersionBean.setSelectable(selectable);
    return this;
  }

  public ModVersionBeanBuilder version(ComparableVersion version) {
    modVersionBean.setVersion(version);
    return this;
  }

  public ModVersionBeanBuilder thumbnailUrl(URL thumbnailUrl) {
    modVersionBean.setThumbnailUrl(thumbnailUrl);
    return this;
  }

  public ModVersionBeanBuilder comments(List<String> comments) {
    modVersionBean.setComments(comments);
    return this;
  }

  public ModVersionBeanBuilder selected(boolean selected) {
    modVersionBean.setSelected(selected);
    return this;
  }

  public ModVersionBeanBuilder played(int played) {
    modVersionBean.setPlayed(played);
    return this;
  }

  public ModVersionBeanBuilder downloadUrl(URL downloadUrl) {
    modVersionBean.setDownloadUrl(downloadUrl);
    return this;
  }

  public ModVersionBeanBuilder mountPoints(List<MountInfo> mountPoints) {
    modVersionBean.setMountPoints(mountPoints);
    return this;
  }

  public ModVersionBeanBuilder hookDirectories(List<String> hookDirectories) {
    modVersionBean.setHookDirectories(hookDirectories);
    return this;
  }

  public ModVersionBeanBuilder reviews(List<ModVersionReviewBean> reviews) {
    modVersionBean.setReviews(reviews);
    return this;
  }

  public ModVersionBeanBuilder reviewsSummary(ModVersionReviewsSummaryBean reviewsSummary) {
    modVersionBean.setReviewsSummary(reviewsSummary);
    return this;
  }

  public ModVersionBeanBuilder modType(ModType modType) {
    modVersionBean.setModType(modType);
    return this;
  }

  public ModVersionBeanBuilder filename(String filename) {
    modVersionBean.setFilename(filename);
    return this;
  }

  public ModVersionBeanBuilder icon(String icon) {
    modVersionBean.setIcon(icon);
    return this;
  }

  public ModVersionBeanBuilder ranked(boolean ranked) {
    modVersionBean.setRanked(ranked);
    return this;
  }

  public ModVersionBeanBuilder hidden(boolean hidden) {
    modVersionBean.setHidden(hidden);
    return this;
  }

  public ModVersionBeanBuilder mod(ModBean mod) {
    modVersionBean.setMod(mod);
    return this;
  }

  public ModVersionBeanBuilder id(Integer id) {
    modVersionBean.setId(id);
    return this;
  }

  public ModVersionBeanBuilder createTime(OffsetDateTime createTime) {
    modVersionBean.setCreateTime(createTime);
    return this;
  }

  public ModVersionBeanBuilder updateTime(OffsetDateTime updateTime) {
    modVersionBean.setUpdateTime(updateTime);
    return this;
  }

  public ModVersionBean get() {
    return modVersionBean;
  }

}

