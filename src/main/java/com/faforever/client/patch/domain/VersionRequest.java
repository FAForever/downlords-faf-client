package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class VersionRequest extends UpdateServerRequest {

  public VersionRequest(String targetDirectoryName, String filename, String targetVersion) {
    super(UpdateServerCommand.REQUEST_VERSION);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(targetVersion);
  }
}
