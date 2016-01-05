package com.faforever.client.patch.domain;

public class IncrementModDownloadCountRequest extends UpdateServerRequest {

  public IncrementModDownloadCountRequest(String uid) {
    super(UpdateServerCommand.ADD_DOWNLOAD_SIM_MOD);
    addArg(uid);
  }
}
