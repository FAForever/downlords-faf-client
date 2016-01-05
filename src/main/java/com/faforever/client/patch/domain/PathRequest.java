package com.faforever.client.patch.domain;

public class PathRequest extends UpdateServerRequest {

  public PathRequest(String targetDirectoryName, String filename) {
    super(UpdateServerCommand.REQUEST_PATH);
    addArg(targetDirectoryName);
    addArg(filename);
  }
}
