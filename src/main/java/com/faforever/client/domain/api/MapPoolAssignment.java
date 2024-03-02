package com.faforever.client.domain.api;

import com.faforever.commons.api.dto.MapParams;

public record MapPoolAssignment(
    Integer id,
    MapParams mapParams, MapPool mapPool, MapVersion mapVersion,
    int weight
) {}
