package com.faforever.client.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Lazy
@Component
@Slf4j
public class SteamService {
  private static final int FA_APP_ID = 9420;

  public SteamService() throws IOException {
    Path steamAppIdPath = Path.of("steam_appid.txt").toAbsolutePath();
    if (!Files.exists(steamAppIdPath)) {
      Files.writeString(steamAppIdPath, String.valueOf(FA_APP_ID));
    }
  }

  public void startSteamApi() {
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

  public void shutdownSteamApi() {
    log.info("Stopping the Steam API");
    if (SteamAPI.isSteamRunning()) {
      SteamAPI.shutdown();
      log.debug("Steam API stopped");
    }
  }

}
