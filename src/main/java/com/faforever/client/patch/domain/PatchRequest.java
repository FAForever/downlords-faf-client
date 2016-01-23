package com.faforever.client.patch.domain;

public class PatchRequest extends UpdateServerRequest {

  public PatchRequest(String targetDirectoryName, String filename, String targetVersion) {
    super(UpdateServerCommand.PATCH_TO);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(targetVersion);
  }
}
