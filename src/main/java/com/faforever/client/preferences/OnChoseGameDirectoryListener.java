package com.faforever.client.preferences;

import com.faforever.client.util.Callback;

import java.nio.file.Path;

public interface OnChoseGameDirectoryListener {

  void onChoseGameDirectory(Callback<Path> callback);
}
