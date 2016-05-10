package com.faforever.client.preferences;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface OnChooseGameDirectoryListener {

  CompletableFuture<Path> onChooseGameDirectory();
}
