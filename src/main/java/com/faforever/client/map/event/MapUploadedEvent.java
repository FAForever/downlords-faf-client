package com.faforever.client.map.event;

import com.faforever.client.domain.MapVersionBean;

public record MapUploadedEvent(MapVersionBean mapInfo) {}
