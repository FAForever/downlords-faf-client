package com.faforever.client.patch.domain;

public class VersionRequest extends UpdateServerRequest {

  public VersionRequest(String targetDirectoryName, String filename, String targetVersion) {
    super(UpdateServerCommand.REQUEST_VERSION);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(targetVersion);
  }
}
