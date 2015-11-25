package com.faforever.client.legacy.domain;

import com.faforever.client.api.Mod;

import java.util.List;

public class ModSearchResult extends ServerMessage {

  private List<Mod> modList;

  public ModSearchResult() {
    super(ServerMessageType.MOD_RESULT_LIST);
  }

  public List<Mod> getModList() {
    return modList;
  }

  public void setModList(List<Mod> modList) {
    this.modList = modList;
  }
}
