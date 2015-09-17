package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class RequestRequest extends UpdateServerRequest {

  public RequestRequest(String targetDirectoryName, String response) {
    super(UpdateServerCommand.REQUEST);
    addArg(targetDirectoryName);
    addArg(response);
  }
}
