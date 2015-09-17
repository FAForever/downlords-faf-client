package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class SimPathRequest extends UpdateServerRequest {

  public SimPathRequest(String modUid) {
    super(UpdateServerCommand.REQUEST_SIM_PATH);
    addArg(modUid);
  }
}
