package com.faforever.client.preferences;

import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class DataPrefs {
  private static final Path DEFAULT_FAF_DATA_DIRECTORY;
  private static final String USER_HOME_SUB_FOLDER = ".faforever";
  private static final String BIN_SUB_FOLDER = "bin";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final String FEATURED_MOD_CACHE_SUB_FOLDER = "featured_mod";
  private static final String CACHE_STYLESHEETS_SUB_FOLDER = Path.of(CACHE_SUB_FOLDER, "stylesheets").toString();
  private static final String THEMES_SUB_FOLDER = "themes";
  private static final String LANGUAGES_SUB_FOLDER = "languages";
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";

  static {
    if (org.bridj.Platform.isWindows()) {
      DEFAULT_FAF_DATA_DIRECTORY = Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
    } else {
      DEFAULT_FAF_DATA_DIRECTORY = Path.of(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
  }

  ObjectProperty<Path> baseDataDirectory = new SimpleObjectProperty<>(DEFAULT_FAF_DATA_DIRECTORY);

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
