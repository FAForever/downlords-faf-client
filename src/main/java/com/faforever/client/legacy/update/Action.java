package com.faforever.client.legacy.update;

enum Action {
  GET_FILES_TO_UPDATE,
  REQUEST_SIM_PATH,
  ADD_DOWNLOAD_SIM_MOD,
  REQUEST_VERSION,
  REQUEST_MOD_VERSION,
  REQUEST;

  String getString() {
    return name();
  }
}
