package com.faforever.client.patch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface UpdateServerAccessor {

  void connect(UpdateServerResponseListener updateServerResponseListener);

  void disconnect();

  CompletionStage<List<String>> requestFilesToUpdate(String fileGroup);

  void requestVersion(String targetDirectoryName, String filename, String targetVersion);

  void requestModVersion(String targetDirectoryName, String filename, Map<String, Integer> modVersions);

  void requestPath(String targetDirectoryName, String filename);

  void patchTo(String targetDirectoryName, String filename, String currentMd5, String targetVersion);

  void modPatchTo(String targetDirectoryName, String filename, String currentMd5, Map<String, Integer> modVersions);

  void update(String targetDirectoryName, String filename, String actualMd5);

  CompletionStage<String> requestSimPath(String uid);

  void incrementModDownloadCount(String uid);

  void request(String targetDirectoryName, String response);
}
