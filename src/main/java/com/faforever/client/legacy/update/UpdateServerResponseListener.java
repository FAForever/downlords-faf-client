package com.faforever.client.legacy.update;

import java.io.IOException;
import java.util.Set;

public interface UpdateServerResponseListener {

  void onSimModPath(String s);

  void onSimModNotFound();

  void onFilesToUpdate(Set<String> files) throws IOException;

  void onUnknownApp();

  void onServerBusy();

  void onVersionPatchNotFound(String s);

  void onModPatchNotFound(String s);

  void onPatchNotFound(String s);

  void onFileUpToDate(String s);

  void onFileNotFound(String s);

  void onSendFilePath(String path, String fileToCopy, String url);

  void onSendFile(String s);
}
