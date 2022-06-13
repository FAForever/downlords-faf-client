package com.faforever.client.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class SteamService implements InitializingBean, DisposableBean {
  private static final String STEAM_ID_FILENAME = "steam_appid.txt";

  public SteamService() {
    Path steamAppIdPath = Path.of(STEAM_ID_FILENAME).toAbsolutePath();
    if (!Files.exists(steamAppIdPath)) {
      try (FileOutputStream outputStream = new FileOutputStream(steamAppIdPath.toFile()); InputStream inputStream = SteamService.class.getResourceAsStream("/" + STEAM_ID_FILENAME)) {
        if (inputStream != null) {
          byte[] steamIdBytes = inputStream.readAllBytes();
          outputStream.write(steamIdBytes);
        }
      } catch (IOException e) {
        log.warn("steam_appid.txt did not already exist and unable to write new one to installation directory. Steam integration will not work");
      }
    }
  }

  public void afterPropertiesSet() {
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

  public void destroy() {
    log.info("Stopping the Steam API");
    if (SteamAPI.isSteamRunning()) {
      SteamAPI.shutdown();
      log.debug("Steam API stopped");
    }
  }

}
