package com.faforever.client.mod;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public class ModInfoBeanBuilder {

  private final ModInfoBean modInfo;

  public ModInfoBeanBuilder() {
    modInfo = new ModInfoBean();
  }

  public ModInfoBeanBuilder defaultValues() {
    modInfo.setPublishDate(LocalDateTime.now());
    uid(UUID.randomUUID().toString());
    return this;
  }

  public ModInfoBeanBuilder uid(String uid) {
    modInfo.setId(uid);
    return this;
  }

  public ModInfoBean get() {
    return modInfo;
  }

  public ModInfoBeanBuilder downloadUrl(URL url) throws MalformedURLException {
    modInfo.setDownloadUrl(url);
    return this;
  }

  public ModInfoBeanBuilder uiMod(boolean uiOnly) {
    modInfo.setUiOnly(uiOnly);
    return this;
  }

  public ModInfoBeanBuilder name(String name) {
    modInfo.setName(name);
    return this;
  }

  public ModInfoBeanBuilder author(String author) {
    modInfo.setAuthor(author);
    return this;
  }

  public ModInfoBeanBuilder thumbnailUrl(String thumbnailUrl) {
    modInfo.setThumbnailUrl(thumbnailUrl);
    return this;
  }

  public static ModInfoBeanBuilder create() {
    return new ModInfoBeanBuilder();
  }
}
