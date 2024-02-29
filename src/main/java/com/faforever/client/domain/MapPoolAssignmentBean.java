package com.faforever.client.domain;

import com.faforever.commons.api.dto.MapParams;

public record MapPoolAssignmentBean(
    Integer id,
    MapParams mapParams,
    MapPoolBean mapPool,
    MapVersionBean mapVersion,
    int weight
) {}
