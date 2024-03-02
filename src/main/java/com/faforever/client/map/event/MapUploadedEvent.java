package com.faforever.client.map.event;

import com.faforever.client.domain.api.MapVersion;

public record MapUploadedEvent(MapVersion mapInfo) {}
