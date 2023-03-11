package com.faforever.client.ui.preferences.event;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GameDirectoryChosenEvent(Path path, Optional<CompletableFuture<Path>> future) {}
