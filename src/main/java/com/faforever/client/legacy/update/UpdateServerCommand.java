package com.faforever.client.legacy.update;

import java.util.HashMap;
import java.util.Map;

public enum UpdateServerCommand {
  PATH_TO_SIM_MOD,
  SIM_MOD_NOT_FOUND,
  LIST_FILES_TO_UP,
  UNKNOWN_APP,
  THIS_PATCH_IS_IN_CREATION_EXCEPTION,
  VERSION_PATCH_NOT_FOUND,
  VERSION_MOD_PATCH_NOT_FOUND,
  PATCH_NOT_FOUND,
  UP_TO_DATE,
  ERROR_FILE,
  SEND_FILE_PATH,
  SEND_FILE;

  private static final Map<String, UpdateServerCommand> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (UpdateServerCommand relayServerCommand : values()) {
      fromString.put(relayServerCommand.getString(), relayServerCommand);
    }
  }

  public String getString() {
    return name();
  }

  public static UpdateServerCommand fromString(String string) {
    UpdateServerCommand updateServerCommand = fromString.get(string);
    if (updateServerCommand == null) {
      /*
       * If an unknown command is received, ignoring it would probably cause the application to enter an unknown state.
       * So it's better to crash right now so there's no doubt that something went wrong.
       */
      throw new IllegalArgumentException("Unknown update server command: " + string);
    }
    return updateServerCommand;
  }
  }
