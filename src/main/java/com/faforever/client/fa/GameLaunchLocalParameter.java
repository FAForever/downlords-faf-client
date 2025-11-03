package com.faforever.client.fa;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public enum GameLaunchLocalParameter {

  INIT_FILE("/init", ForgedAlliancePrefs.INIT_FILE_NAME),
  NO_BUG_REPORT("/nobugreport"),
  GAME_OVER("/exitongameover"),
  NO_SOUND("/nosound");

  private final String parameterName;
  private final String value;

  GameLaunchLocalParameter(String parameterName, String value) {
    this.parameterName = parameterName;
    this.value = value;
  }

  GameLaunchLocalParameter(String parameterName) {
    this(parameterName, "");
  }

  public List<String> get() {
    return StringUtils.isNotBlank(value) ? List.of(parameterName, value) : List.of(parameterName);
  }
}
