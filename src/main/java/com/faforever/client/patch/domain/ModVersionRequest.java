package com.faforever.client.patch.domain;

public class ModVersionRequest extends UpdateServerRequest {

  public ModVersionRequest(String targetDirectoryName, String filename, String modVersions) {
    super(UpdateServerCommand.REQUEST_MOD_VERSION);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(modVersions);
  }
}
