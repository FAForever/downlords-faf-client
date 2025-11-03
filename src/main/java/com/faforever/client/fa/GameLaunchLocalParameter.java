package com.faforever.client.fa;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public enum GameLaunchLocalParameter {

  INIT_FILE("/init", ForgedAlliancePrefs.INIT_FILE_NAME),
  NO_BUG_REPORT("/nobugreport"),
  GAME_OVER("/exitongameover"),
  NO_SOUND("/nosound");

  private final String name;
  private final String value;

  GameLaunchLocalParameter(String name, String value) {
    this.name = name;
    this.value = value;
  }

  GameLaunchLocalParameter(String name) {
    this(name, "");
  }

  public List<String> getCommand() {
    return StringUtils.isNotBlank(value) ? List.of(name, value) : List.of(name);
  }
}
