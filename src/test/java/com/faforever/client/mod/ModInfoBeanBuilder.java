package com.faforever.client.mod;


import com.faforever.commons.mod.MountInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.nocatch.NoCatch.noCatch;

public class ModInfoBeanBuilder {

  private final ModVersion modVersionInfo;

  public ModInfoBeanBuilder() {
    modVersionInfo = new ModVersion();
  }

  public static ModInfoBeanBuilder create() {
    return new ModInfoBeanBuilder();
  }

  public ModInfoBeanBuilder defaultValues() {
    modVersionInfo.setCreateTime(LocalDateTime.now());
    Mod mod = new Mod();
    modVersionInfo.setMod(mod);
    name("ModVersion");
    uid(UUID.randomUUID().toString());
    version(new ComparableVersion("1"));
    return this;
  }

  public ModInfoBeanBuilder version(ComparableVersion version) {
    modVersionInfo.setVersion(version);
    return this;
  }

  public ModInfoBeanBuilder uid(String uid) {
    modVersionInfo.setUid(uid);
    return this;
  }

  public ModVersion get() {
    return modVersionInfo;
  }

  public ModInfoBeanBuilder downloadUrl(URL url) {
    modVersionInfo.setDownloadUrl(url);
    return this;
  }

  public ModInfoBeanBuilder name(String name) {
    modVersionInfo.setDisplayName(name);
    return this;
  }

  public ModInfoBeanBuilder author(String author) {
    if (modVersionInfo.getMod() == null) {
      modVersionInfo.setMod(new Mod());
    }
    modVersionInfo.getMod().setAuthor(author);
    return this;
  }

  public ModInfoBeanBuilder thumbnailUrl(String thumbnailUrl) {
    modVersionInfo.setThumbnailUrl(noCatch(() -> thumbnailUrl == null ? null : new URL(thumbnailUrl)));
    return this;
  }

  public ModInfoBeanBuilder mountPoints(List<MountInfo> mountPoints) {
    modVersionInfo.getMountInfos().setAll(mountPoints);
    return this;
  }

  public ModInfoBeanBuilder modType(ModVersion.ModType modType) {
    modVersionInfo.setModType(modType);
    return this;
  }
}
