package com.faforever.client.api;

import com.faforever.commons.api.dto.CoturnServer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IceSession(@NotNull String id, @NotNull List<CoturnServer> servers) {
}
