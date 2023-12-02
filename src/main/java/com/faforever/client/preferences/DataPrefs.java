package com.faforever.client.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;


public class DataPrefs {
  private static final String BIN_SUB_FOLDER = "bin";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String REPLAY_DATA_SUB_FOLDER = "replaydata";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final String FEATURED_MOD_CACHE_SUB_FOLDER = "featured_mod";
  private static final String CACHE_STYLESHEETS_SUB_FOLDER = Path.of(CACHE_SUB_FOLDER, "stylesheets").toString();
  private static final String THEMES_SUB_FOLDER = "themes";
  private static final String LANGUAGES_SUB_FOLDER = "languages";
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";

  private final ObjectProperty<Path> baseDataDirectory = new SimpleObjectProperty<>();

  public Path getBaseDataDirectory() {
    return baseDataDirectory.get();
  }

  public ObjectProperty<Path> baseDataDirectoryProperty() {
    return baseDataDirectory;
  }

  public void setBaseDataDirectory(Path baseDataDirectory) {
    this.baseDataDirectory.set(baseDataDirectory);
  }

  public Path getBinDirectory() {
    return getBaseDataDirectory().resolve(BIN_SUB_FOLDER);
  }

  public Path getReplayDataDirectory() {
    return getBaseDataDirectory().resolve(REPLAY_DATA_SUB_FOLDER);
  }

  public Path getReplayBinDirectory() {
    return getBaseDataDirectory().resolve(REPLAY_DATA_SUB_FOLDER).resolve(BIN_SUB_FOLDER);
  }

  public Path getCacheDirectory() {
    return getBaseDataDirectory().resolve(CACHE_SUB_FOLDER);
  }

  public Path getReplaysDirectory() {
    return getBaseDataDirectory().resolve(REPLAYS_SUB_FOLDER);
  }

  public Path getCorruptedReplaysDirectory() {
    return getReplaysDirectory().resolve(CORRUPTED_REPLAYS_SUB_FOLDER);
  }

  public Path getCacheStylesheetsDirectory() {
    return getBaseDataDirectory().resolve(CACHE_STYLESHEETS_SUB_FOLDER);
  }

  public Path getThemesDirectory() {
    return getBaseDataDirectory().resolve(THEMES_SUB_FOLDER);
  }

  public Path getFeaturedModCacheDirectory() {
    return getCacheDirectory().resolve(FEATURED_MOD_CACHE_SUB_FOLDER);
  }

  public Path getLanguagesDirectory() {
    return getBaseDataDirectory().resolve(LANGUAGES_SUB_FOLDER);
  }

  public Path getMapGeneratorDirectory() {
    return getBaseDataDirectory().resolve(GENERATOR_EXECUTABLE_SUB_DIRECTORY);
  }
}
