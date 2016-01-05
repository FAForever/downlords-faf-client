package com.faforever.client.patch.domain;

public class SimPathRequest extends UpdateServerRequest {

  public SimPathRequest(String modUid) {
    super(UpdateServerCommand.REQUEST_SIM_PATH);
    addArg(modUid);
  }
}
