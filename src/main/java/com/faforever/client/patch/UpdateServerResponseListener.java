package com.faforever.client.patch;

public interface UpdateServerResponseListener {

  void onFileUpToDate(String file);

  void onFileUrl(String targetDirectoryName, String fileToCopy, String url);

  void onPatchUrl(String targetDirectoryName, String fileToUpdate, String url);

  void onVersionPatchNotFound(String response);

  void onVersionModPatchNotFound(String response);

  void onPatchNotFound(String response);
}
