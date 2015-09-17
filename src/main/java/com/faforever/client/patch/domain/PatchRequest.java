package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class PatchRequest extends UpdateServerRequest {

  public PatchRequest(String targetDirectoryName, String filename, String targetVersion) {
    super(UpdateServerCommand.PATCH_TO);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(targetVersion);
  }
}
