package com.faforever.client.builders;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.map.MapSize;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;


public class MapVersionBeanBuilder {
  public static MapVersionBeanBuilder create() {
    return new MapVersionBeanBuilder();
  }

  private final MapVersionBean mapVersionBean = new MapVersionBean();

  public MapVersionBeanBuilder defaultValues() {
    folderName("test.v0000");
    gamesPlayed(0);
    description("test map");
    maxPlayers(2);
    size(MapSize.valueOf(512, 512));
    version(new ComparableVersion("1"));
    hidden(false);
    ranked(true);
    id(0);
    map(MapBeanBuilder.create().defaultValues().latestVersion(mapVersionBean).get());
    return this;
  }

  public MapVersionBeanBuilder folderName(String folderName) {
    mapVersionBean.setFolderName(folderName);
    return this;
  }

  public MapVersionBeanBuilder gamesPlayed(int gamesPlayed) {
    mapVersionBean.setGamesPlayed(gamesPlayed);
    return this;
  }

  public MapVersionBeanBuilder description(String description) {
    mapVersionBean.setDescription(description);
    return this;
  }

  public MapVersionBeanBuilder maxPlayers(int maxPlayers) {
    mapVersionBean.setMaxPlayers(maxPlayers);
    return this;
  }

  public MapVersionBeanBuilder size(MapSize size) {
    mapVersionBean.setSize(size);
    return this;
  }

  public MapVersionBeanBuilder version(ComparableVersion version) {
    mapVersionBean.setVersion(version);
    return this;
  }

  public MapVersionBeanBuilder hidden(boolean hidden) {
    mapVersionBean.setHidden(hidden);
    return this;
  }

  public MapVersionBeanBuilder ranked(boolean ranked) {
    mapVersionBean.setRanked(ranked);
    return this;
  }

  public MapVersionBeanBuilder downloadUrl(URL downloadUrl) {
    mapVersionBean.setDownloadUrl(downloadUrl);
    return this;
  }

  public MapVersionBeanBuilder thumbnailUrlSmall(URL thumbnailUrlSmall) {
    mapVersionBean.setThumbnailUrlSmall(thumbnailUrlSmall);
    return this;
  }

  public MapVersionBeanBuilder thumbnailUrlLarge(URL thumbnailUrlLarge) {
    mapVersionBean.setThumbnailUrlLarge(thumbnailUrlLarge);
    return this;
  }

  public MapVersionBeanBuilder map(MapBean map) {
    mapVersionBean.setMap(map);
    return this;
  }

  public MapVersionBeanBuilder reviews(List<MapVersionReviewBean> reviews) {
    mapVersionBean.setReviews(reviews);
    return this;
  }

  public MapVersionBeanBuilder id(Integer id) {
    mapVersionBean.setId(id);
    return this;
  }

  public MapVersionBeanBuilder createTime(OffsetDateTime createTime) {
    mapVersionBean.setCreateTime(createTime);
    return this;
  }

  public MapVersionBeanBuilder updateTime(OffsetDateTime updateTime) {
    mapVersionBean.setUpdateTime(updateTime);
    return this;
  }

  public MapVersionBean get() {
    return mapVersionBean;
  }

}

