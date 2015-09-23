package com.faforever.client.patch.domain;

import com.faforever.client.patch.UpdateServerCommand;

public class IncrementModDownloadCountRequest extends UpdateServerRequest {

  public IncrementModDownloadCountRequest(String uid) {
    super(UpdateServerCommand.ADD_DOWNLOAD_SIM_MOD);
    addArg(uid);
  }
}
