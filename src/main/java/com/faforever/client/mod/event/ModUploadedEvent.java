package com.faforever.client.mod.event;

import com.faforever.client.domain.ModVersionBean;

public record ModUploadedEvent(ModVersionBean modVersionInfo) {
}
