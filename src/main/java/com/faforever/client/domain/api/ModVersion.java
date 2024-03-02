package com.faforever.client.domain.api;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;

public record ModVersion(
    Integer id,
    String uid,
    String description,
    ComparableVersion version,
    URL thumbnailUrl,
    URL downloadUrl,
    ModType modType,
    boolean ranked,
    boolean hidden, Mod mod,
    OffsetDateTime createTime,
    OffsetDateTime updateTime
) {}
