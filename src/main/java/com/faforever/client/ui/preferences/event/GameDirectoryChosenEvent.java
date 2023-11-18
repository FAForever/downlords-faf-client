package com.faforever.client.ui.preferences.event;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public record GameDirectoryChosenEvent(Path path, CompletableFuture<Path> future) {}
