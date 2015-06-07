package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.htmlparser.HtmlParser;
import com.faforever.client.preferences.PreferencesService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LegacyMapService implements MapService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  HtmlParser htmlParser;

  public LegacyMapService() {
    gson = new GsonBuilder().create();
  }

  @Override
  @Cacheable("smallMapPreview")
  public Image loadSmallPreview(String mapName) {
    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.small"));

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return new Image(url, true);
  }

  @Override
  @Cacheable("largeMapPreview")
  public Image loadLargePreview(String mapName) {
    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.large"));

    logger.debug("Fetching large preview for map {} from {}", mapName, url);

    return new Image(url, true);
  }

  @Override
  public List<MapInfoBean> getMapsFromVault(int page, int maxEntries) {
    // FIXME to background thread

    List<MapInfoBean> maps = FXCollections.observableArrayList();

    MapVaultHtmlContentHandler mapVaultHtmlContentHandler = new MapVaultHtmlContentHandler();

    String urlString = environment.getProperty("vault.mapQueryUrl");
    String params = String.format(environment.getProperty("vault.mapQueryParams"),
        page, maxEntries
    );

    try {
      URL url = new URL(urlString + "?" + params);
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.beginObject();

        while (jsonReader.hasNext()) {
          String key = jsonReader.nextName();
          if (!"layout".equals(key)) {
            jsonReader.skipValue();
            continue;
          }

          String layout = jsonReader.nextString();
          return htmlParser.parse(layout, mapVaultHtmlContentHandler);
        }

        jsonReader.endObject();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return maps;
  }

  @Override
  public ObservableList<MapInfoBean> getLocalMaps() {
    ObservableList<MapInfoBean> maps = FXCollections.observableArrayList();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(preferencesService.getMapsDirectory())) {
      for (Path path : stream) {
        String mapName = path.getFileName().toString();
        maps.add(new MapInfoBean(mapName));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return maps;
  }

  private static String getMapUrl(String mapName, String baseUrl) {
    return StringEscapeUtils.escapeHtml4(String.format(baseUrl, mapName));
  }
}
