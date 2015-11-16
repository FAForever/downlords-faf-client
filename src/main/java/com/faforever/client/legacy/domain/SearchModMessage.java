package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public class SearchModMessage extends ClientMessage {

  public enum ModType {

    SIM(0),
    UI(1),
    ALL(2);

    private static final Map<Integer, ModType> fromCode;

    static {
      fromCode = new HashMap<>();
      for (ModType modType : fromCode.values()) {
        fromCode.put(modType.code, modType);
      }
    }

    // Because brainfuck
    private int code;

    ModType(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }

    public static ModType fromCode(int code) {
      return null;
    }
  }

  private String search;
  private ModType typemod;

  public SearchModMessage(String name, ModType modType) {
    super(ClientMessageType.MOD_VAULT_SEARCH);
    search = name;
    typemod = modType;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public ModType getTypemod() {
    return typemod;
  }

  public void setTypemod(ModType typemod) {
    this.typemod = typemod;
  }
}
