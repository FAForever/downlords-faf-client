package com.faforever.client.patch.domain;

public class PatchRequest extends UpdateServerRequest {

  public PatchRequest(String targetDirectoryName, String filename, String currentMd5, String targetVersion) {
    super(UpdateServerCommand.PATCH_TO);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(currentMd5);
    addArg(targetVersion);
  }
}
