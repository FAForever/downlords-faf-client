package com.faforever.client.api;

import com.faforever.commons.api.dto.CoturnServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record IceSession(
    @NotNull String id, boolean forceRelay, @NotNull List<CoturnServer> servers
) {
}
