package com.faforever.client.map;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

public class MapBeanBuilder {

  private final MapBean mapBean;

  public MapBeanBuilder() {
    mapBean = new MapBean();
  }

  public static MapBeanBuilder create() {
    return new MapBeanBuilder();
  }

  public MapBeanBuilder defaultValues() throws MalformedURLException {
    uid("test");
    displayName("Test Map");
    folderName("testMap");
    author("tester");
    players(4);
    ranked(true);
    hidden(false);
    size(MapSize.valueOf(512, 512));
    createTime(LocalDateTime.MIN);
    description("This is a test map");
    downloadUrl(new URL("http://www.google.com"));
    largeThumbnailUrl(new URL("http://www.google.com"));
    version(new ComparableVersion("1"));
    return this;
  }

  public MapBeanBuilder displayName(String name) {
    mapBean.setDisplayName(name);
    return this;
  }

  public MapBeanBuilder folderName(String name) {
    mapBean.setFolderName(name);
    return this;
  }

  public MapBeanBuilder uid(String uid) {
    mapBean.setId(uid);
    return this;
  }

  public MapBean get() {
    return mapBean;
  }

  public MapBeanBuilder downloadUrl(URL url) {
    mapBean.setDownloadUrl(url);
    return this;
  }

  public MapBeanBuilder author(String author) {
    mapBean.setAuthor(author);
    return this;
  }

  public MapBeanBuilder smallThumbnailUrl(URL thumbnailUrl) {
    mapBean.setSmallThumbnailUrl(thumbnailUrl);
    return this;
  }

  public MapBeanBuilder largeThumbnailUrl(URL thumbnailUrl) {
    mapBean.setLargeThumbnailUrl(thumbnailUrl);
    return this;
  }

  public MapBeanBuilder ranked(boolean ranked) {
    mapBean.setRanked(ranked);
    return this;
  }

  public MapBeanBuilder hidden(boolean hidden) {
    mapBean.setHidden(hidden);
    return this;
  }

  public MapBeanBuilder size(MapSize size) {
    mapBean.setSize(size);
    return this;
  }

  public MapBeanBuilder version(ComparableVersion version) {
    mapBean.setVersion(version);
    return this;
  }

  public MapBeanBuilder version(int version) {
    mapBean.setVersion(new ComparableVersion(String.valueOf(version)));
    return this;
  }

  public MapBeanBuilder players(int players) {
    mapBean.setPlayers(players);
    return this;
  }

  public MapBeanBuilder createTime(LocalDateTime createTime) {
    mapBean.setCreateTime(createTime);
    return this;
  }

  public MapBeanBuilder description(String description) {
    mapBean.setDescription(description);
    return this;
  }
}
