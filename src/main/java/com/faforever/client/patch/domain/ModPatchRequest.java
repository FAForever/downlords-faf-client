package com.faforever.client.patch.domain;

public class ModPatchRequest extends UpdateServerRequest {

  public ModPatchRequest(String targetDirectoryName, String filename, String modVersionsJson) {
    super(UpdateServerCommand.MOD_PATCH_TO);
    addArg(targetDirectoryName);
    addArg(filename);
    addArg(modVersionsJson);
  }
}
