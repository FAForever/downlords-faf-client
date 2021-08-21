package com.faforever.client.builders;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapBean.MapType;
import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;
import java.util.List;


public class MapBeanBuilder {
  public static MapBeanBuilder create() {
    return new MapBeanBuilder();
  }

  private final MapBean mapBean = new MapBean();

  public MapBeanBuilder defaultValues() {
    displayName("test");
    gamesPlayed(10);
    author(PlayerBeanBuilder.create().defaultValues().get());
    recommended(true);
    id(0);
    mapType(MapType.SKIRMISH);
    versions(List.of());
    return this;
  }

  public MapBeanBuilder displayName(String displayName) {
    mapBean.setDisplayName(displayName);
    return this;
  }

  public MapBeanBuilder gamesPlayed(int gamesPlayed) {
    mapBean.setGamesPlayed(gamesPlayed);
    return this;
  }

  public MapBeanBuilder author(PlayerBean author) {
    mapBean.setAuthor(author);
    return this;
  }

  public MapBeanBuilder recommended(boolean recommended) {
    mapBean.setRecommended(recommended);
    return this;
  }

  public MapBeanBuilder mapType(MapType mapType) {
    mapBean.setMapType(mapType);
    return this;
  }

  public MapBeanBuilder latestVersion(MapVersionBean latestVersion) {
    mapBean.setLatestVersion(latestVersion);
    return this;
  }

  public MapBeanBuilder mapReviewsSummary(MapReviewsSummaryBean mapReviewsSummary) {
    mapBean.setMapReviewsSummary(mapReviewsSummary);
    return this;
  }

  public MapBeanBuilder versions(List<MapVersionBean> versions) {
    mapBean.setVersions(versions);
    return this;
  }

  public MapBeanBuilder id(Integer id) {
    mapBean.setId(id);
    return this;
  }

  public MapBeanBuilder createTime(OffsetDateTime createTime) {
    mapBean.setCreateTime(createTime);
    return this;
  }

  public MapBeanBuilder updateTime(OffsetDateTime updateTime) {
    mapBean.setUpdateTime(updateTime);
    return this;
  }

  public MapBean get() {
    return mapBean;
  }

}

