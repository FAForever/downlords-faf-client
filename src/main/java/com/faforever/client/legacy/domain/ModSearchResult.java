package com.faforever.client.legacy.domain;

import java.util.List;

public class ModSearchResult extends ServerMessage {

  private List<ModInfo> modList;

  public ModSearchResult() {
    super(ServerMessageType.MOD_RESULT_LIST);
  }

  public List<ModInfo> getModList() {
    return modList;
  }

  public void setModList(List<ModInfo> modList) {
    this.modList = modList;
  }
}
