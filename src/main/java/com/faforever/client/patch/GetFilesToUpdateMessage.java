package com.faforever.client.patch;

import com.faforever.client.patch.domain.UpdateServerRequest;

public class GetFilesToUpdateMessage extends UpdateServerRequest {

  public GetFilesToUpdateMessage(String fileGroup) {
    super(UpdateServerCommand.GET_FILES_TO_UPDATE);
    addArg(fileGroup);
  }
}
