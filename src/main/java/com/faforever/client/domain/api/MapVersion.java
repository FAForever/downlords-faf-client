package com.faforever.client.domain.api;

import com.faforever.client.map.MapSize;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;

public record MapVersion(
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
    Map map,
    OffsetDateTime createTime
) {}
