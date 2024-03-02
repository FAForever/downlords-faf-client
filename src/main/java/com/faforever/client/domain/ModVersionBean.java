package com.faforever.client.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;

public record ModVersionBean(
    Integer id,
    String uid,
    String description,
    ComparableVersion version,
    URL thumbnailUrl,
    URL downloadUrl,
    ModType modType,
    boolean ranked,
    boolean hidden,
    ModBean mod,
    OffsetDateTime createTime,
    OffsetDateTime updateTime
) {

  @RequiredArgsConstructor
  @Getter
  public enum ModType {
    UI("modType.ui"),
    SIM("modType.sim");

    private final String i18nKey;
  }
}
