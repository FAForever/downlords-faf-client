package com.faforever.client.preferences;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

public interface OnChooseGameDirectoryListener {

  CompletionStage<Path> onChooseGameDirectory();
}
