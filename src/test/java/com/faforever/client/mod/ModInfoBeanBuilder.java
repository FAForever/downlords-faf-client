package com.faforever.client.mod;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

public class ModInfoBeanBuilder {

  private final ModInfoBean modInfo;

  public ModInfoBeanBuilder() {
    modInfo = new ModInfoBean();
  }

  public ModInfoBeanBuilder defaultValues() {
    modInfo.setPublishDate(Instant.now());
    return this;
  }

  public ModInfoBeanBuilder uid(String uid) {
    modInfo.setUid(uid);
    return this;
  }

  public ModInfoBean get() {
    return modInfo;
  }

  public ModInfoBeanBuilder downloadUrl(URL url) throws MalformedURLException {
    modInfo.setDownloadUrl(url);
    return this;
  }

  public ModInfoBeanBuilder uidMod(boolean uiOnly) {
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
