package com.faforever.client.domain;

import com.faforever.client.map.MapSize;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;

public record MapVersionBean(
    Integer id,
    String folderName,
    int gamesPlayed,
    String description,
    int maxPlayers,
    MapSize size,
    ComparableVersion version,
    boolean hidden,
    boolean ranked,
    URL downloadUrl,
    URL thumbnailUrlSmall,
    URL thumbnailUrlLarge,
    MapBean map,
    OffsetDateTime createTime
) {}
