package com.faforever.client.patch.domain;

public class RequestRequest extends UpdateServerRequest {

  public RequestRequest(String targetDirectoryName, String response) {
    super(UpdateServerCommand.REQUEST);
    addArg(targetDirectoryName);
    addArg(response);
  }
}
