package com.faforever.client.legacy.update;

import java.util.List;

public class UpdateServerMessage {

  /**
   * Contains the command to execute, but the server sends it as "key".
   */
  public UpdateServerCommand key;

  /**
   * Contains the arguments, but the server thinks it's cool to confuse us by calling it "commands".
   */
  public List<Object> commands;
}
