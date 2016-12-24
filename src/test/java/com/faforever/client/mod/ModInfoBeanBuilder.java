package com.faforever.client.mod;

import com.faforever.client.patch.MountPoint;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.nocatch.NoCatch.noCatch;

public class ModInfoBeanBuilder {

  private final Mod modInfo;

  public ModInfoBeanBuilder() {
    modInfo = new Mod();
  }

  public static ModInfoBeanBuilder create() {
    return new ModInfoBeanBuilder();
  }

  public ModInfoBeanBuilder defaultValues() {
    modInfo.setPublishDate(LocalDateTime.now());
    name("Mod");
    uid(UUID.randomUUID().toString());
    version(new ComparableVersion("1"));
    return this;
  }

  public ModInfoBeanBuilder version(ComparableVersion version) {
    modInfo.setVersion(version);
    return this;
  }

  public ModInfoBeanBuilder uid(String uid) {
    modInfo.setId(uid);
    return this;
  }

  public Mod get() {
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
    modInfo.setThumbnailUrl(noCatch(() -> thumbnailUrl == null ? null : new URL(thumbnailUrl)));
    return this;
  }

  public ModInfoBeanBuilder mountPoints(List<MountPoint> mountPoints) {
    modInfo.getMountPoints().setAll(mountPoints);
    return this;
  }
}
