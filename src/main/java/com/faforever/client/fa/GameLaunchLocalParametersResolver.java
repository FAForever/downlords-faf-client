package com.faforever.client.fa;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import javafx.beans.property.BooleanProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GameLaunchLocalParametersResolver {

  private final Set<GameLaunchLocalParameter> parameters = new HashSet<>();

  public GameLaunchLocalParametersResolver(Preferences preferences) {
    ForgedAlliancePrefs faPrefs = preferences.getForgedAlliance();

    subscribe(faPrefs.skipStatisticProperty(), GameLaunchLocalParameter.GAME_OVER);
    subscribe(faPrefs.noGameSoundProperty(), GameLaunchLocalParameter.NO_SOUND);
  }

  private void subscribe(BooleanProperty property, GameLaunchLocalParameter parameter) {
    property.subscribe(newValue -> resolveParameter(parameter, newValue));
  }

  private void resolveParameter(GameLaunchLocalParameter parameter, boolean state) {
    if (state) {
      parameters.add(parameter);
    } else {
      parameters.remove(parameter);
    }
  }

  public List<GameLaunchLocalParameter> getParameters() {
    return new ArrayList<>(parameters);
  }
}
