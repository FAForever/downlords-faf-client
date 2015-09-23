package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class UpdateFileRequest extends UpdateServerRequest {

  public UpdateFileRequest(String targetDirectory, String filename, String actualMd5) {
    super(UpdateServerCommand.UPDATE);
    addArg(targetDirectory);
    addArg(filename);
    addArg(actualMd5);
  }
}
