package com.faforever.client.map;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class MapBeanBuilder {

  private final MapBean mapBean;

  public MapBeanBuilder() {
    mapBean = new MapBean();
  }

  public MapBeanBuilder defaultValues() {
    uid(UUID.randomUUID().toString())
        .displayName("Map Name");
    return this;
  }

  public MapBeanBuilder displayName(String name) {
    mapBean.setDisplayName(name);
    return this;
  }

  public MapBeanBuilder uid(String uid) {
    mapBean.setId(uid);
    return this;
  }

  public MapBean get() {
    return mapBean;
  }

  public MapBeanBuilder downloadUrl(URL url) throws MalformedURLException {
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

  public static MapBeanBuilder create() {
    return new MapBeanBuilder();
  }
}
