package com.faforever.client.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import com.faforever.client.preferences.GeneralPrefs;
import com.faforever.client.preferences.Preferences;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SteamService implements InitializingBean, DisposableBean {

  private final GeneralPrefs generalPrefs;

  public SteamService(Preferences preferences) {
    this.generalPrefs = preferences.getGeneral();
  }

  @Override
  public void afterPropertiesSet() {
    if (generalPrefs.getDisableSteamStart()) {
      return;
    }

    try {
      log.info("Starting the Steam API");
      SteamAPI.loadLibraries();
      if (SteamAPI.init()) {
        log.debug("Steam API started");
      } else {
        log.debug("Steam API failed to start");
      }
    } catch (SteamException e) {
      log.warn("Unable to start Steam API", e);
    }
  }

  @Override
  public void destroy() {
    log.info("Stopping the Steam API");
    if (SteamAPI.isSteamRunning()) {
      SteamAPI.shutdown();
      log.debug("Steam API stopped");
    }
  }

}
