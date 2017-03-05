package com.faforever.client.theme;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.github.nocatch.NoCatch.NoCatchRunnable;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.io.FileUtils.deleteRecursively;
import static com.faforever.client.preferences.Preferences.DEFAULT_THEME_NAME;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


@Lazy
@Service
public class UiServiceImpl implements UiService {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * This value needs to be updated whenever theme-breaking changes were made to the client.
   */
  private static final int THEME_VERSION = 1;
  private static final String METADATA_FILE_NAME = "theme.properties";
  private final Set<Scene> scenes;
  private final Set<WebView> webViews;

  private final PreferencesService preferencesService;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final CacheManager cacheManager;
  private final MessageSource messageSource;
  private final ApplicationContext applicationContext;
  private final I18n i18n;

  private WatchService watchService;
  private ObservableMap<String, Theme> themesByFolderName;
  private Map<Theme, String> folderNamesByTheme;
  private Map<Path, WatchKey> watchKeys;
  private ObjectProperty<Theme> currentTheme;
  private Path currentTempStyleSheet;
  private MessageSourceResourceBundle resources;

  @Inject
  public UiServiceImpl(PreferencesService preferencesService, ThreadPoolExecutor threadPoolExecutor,
                       CacheManager cacheManager, MessageSource messageSource, ApplicationContext applicationContext,
                       I18n i18n) {
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.threadPoolExecutor = threadPoolExecutor;
    this.cacheManager = cacheManager;
    this.messageSource = messageSource;
    this.applicationContext = applicationContext;

    scenes = Collections.synchronizedSet(new HashSet<>());
    webViews = new HashSet<>();
    watchKeys = new HashMap<>();
    currentTheme = new SimpleObjectProperty<>(DEFAULT_THEME);
    folderNamesByTheme = new HashMap<>();
    themesByFolderName = FXCollections.observableHashMap();
    themesByFolderName.addListener((MapChangeListener<String, Theme>) change -> {
      if (change.wasRemoved()) {
        folderNamesByTheme.remove(change.getValueRemoved());
      }
      if (change.wasAdded()) {
        folderNamesByTheme.put(change.getValueAdded(), change.getKey());
      }
    });
  }

  @PostConstruct
  void postConstruct() throws IOException, InterruptedException {
    resources = new MessageSourceResourceBundle(messageSource, i18n.getUserSpecificLocale());
    Path themesDirectory = preferencesService.getThemesDirectory();
    startWatchService(themesDirectory);
    Path cacheStylesheetsDirectory = preferencesService.getCacheStylesheetsDirectory();
    if (Files.exists(cacheStylesheetsDirectory)) {
      deleteRecursively(cacheStylesheetsDirectory);
    }
    loadThemes();

    String storedTheme = preferencesService.getPreferences().getThemeName();
    setTheme(themesByFolderName.get(storedTheme));
  }

  private void startWatchService(Path themesDirectory) throws IOException, InterruptedException {
    watchService = themesDirectory.getFileSystem().newWatchService();
    threadPoolExecutor.execute(() -> {
      try {
        while (!Thread.interrupted()) {
          WatchKey key = watchService.take();
          onWatchEvent(key);
          key.reset();
        }
      } catch (InterruptedException | ClosedWatchServiceException e) {
        logger.debug("Watcher service terminated");
      } catch (IOException e) {
        logger.warn("Exception while watching directories", e);
      }
    });
  }

  private void addThemeDirectory(Path path) {
    Path metadataFile = path.resolve(METADATA_FILE_NAME);
    if (Files.notExists(metadataFile)) {
      return;
    }

    try (Reader reader = Files.newBufferedReader(metadataFile)) {
      String folderName = path.getFileName().toString();
      themesByFolderName.put(folderName, readTheme(reader));
    } catch (IOException e) {
      logger.warn("Theme could not be read: " + metadataFile.toAbsolutePath(), e);
    }
  }

  private Theme readTheme(Reader reader) throws IOException {
    Properties properties = new Properties();
    properties.load(reader);
    return Theme.fromProperties(properties);
  }

  @PreDestroy
  void preDestroy() {
    IOUtils.closeQuietly(watchService);
  }

  private void stopWatchingTheme(Theme theme) {
    Path path = getThemeDirectory(theme);
    if (watchKeys.containsKey(path)) {
      watchKeys.remove(path).cancel();
    }
  }

  /**
   * Watches all contents in the specified theme for changes and reloads the theme if a change is detected.
   */
  private void watchTheme(Theme theme) {
    Path themePath = getThemeDirectory(theme);
    logger.debug("Watching theme directory for changes: {}", themePath.toAbsolutePath());
    noCatch(() -> Files.walkFileTree(themePath, new DirectoryVisitor(path -> watchDirectory(themePath, watchService))));
  }

  private void onWatchEvent(WatchKey key) throws IOException {
    for (WatchEvent<?> watchEvent : key.pollEvents()) {
      Path path = (Path) watchEvent.context();
      if (watchEvent.kind() == ENTRY_CREATE && Files.isDirectory(path)) {
        watchDirectory(path, watchService);
      } else if (watchEvent.kind() == ENTRY_DELETE && Files.isDirectory(path)) {
        watchKeys.remove(path);
      }
    }

    reloadStylesheet();
  }

  private void watchDirectory(Path directory, WatchService watchService) {
    if (watchKeys.containsKey(directory)) {
      return;
    }
    logger.debug("Watching directory: {}", directory.toAbsolutePath());
    noCatch(() -> watchKeys.put(directory, directory.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)));
  }

  private void reloadStylesheet() {
    String styleSheet = getSceneStyleSheet();

    logger.debug("Changes detected, reloading stylesheet: {}", styleSheet);
    scenes.forEach(scene -> setSceneStyleSheet(scene, styleSheet));
    setAndCreateWebViewsStyleSheet(getWebViewStyleSheet());
  }

  private void setSceneStyleSheet(Scene scene, String styleSheet) {
    Platform.runLater(() -> scene.getStylesheets().setAll(styleSheet));
  }

  private String getSceneStyleSheet() {
    return getThemeFile(STYLE_CSS);
  }

  @Override
  public String getThemeFile(String relativeFile) {
    String strippedRelativeFile = relativeFile.replace("theme/", "");
    Path externalFile = getThemeDirectory(currentTheme.get()).resolve(strippedRelativeFile);
    if (Files.notExists(externalFile)) {
      return noCatch(() -> new ClassPathResource("/" + relativeFile).getURL().toString());
    }
    return noCatch(() -> externalFile.toUri().toURL().toString());
  }

  @Override
  @Cacheable(CacheNames.THEME_IMAGES)
  public Image getThemeImage(String relativeImage) {
    return new Image(getThemeFile(relativeImage), true);
  }


  @Override
  public URL getThemeFileUrl(String relativeFile) {
    String themeFile = getThemeFile(relativeFile);
    if (themeFile.startsWith("file:") || themeFile.startsWith("jar:")) {
      return noCatch(() -> new URL(themeFile));
    }
    return noCatch(() -> new ClassPathResource(getThemeFile(relativeFile)).getURL());
  }


  @Override
  public void setTheme(Theme theme) {
    stopWatchingTheme(theme);

    if (theme == DEFAULT_THEME) {
      preferencesService.getPreferences().setThemeName(DEFAULT_THEME_NAME);
    } else {
      watchTheme(theme);
      preferencesService.getPreferences().setThemeName(getThemeDirectory(theme).getFileName().toString());
    }
    preferencesService.storeInBackground();
    reloadStylesheet();
    currentTheme.set(theme);
    cacheManager.getCache(CacheNames.THEME_IMAGES).clear();
  }

  @Override
  public void unregisterScene(Scene scene) {
    scenes.remove(scene);
  }

  @Override
  public void registerScene(Scene scene) {
    scenes.add(scene);

    scene.getWindow().showingProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        unregisterScene(scene);
      } else {
        registerScene(scene);
      }
    });
    scene.getStylesheets().setAll(getStylesheets());
  }

  @Override
  public String[] getStylesheets() {
    return new String[]{getSceneStyleSheet(), getThemeFile("theme/material-colors.css")};
  }

  @Override
  public void registerWebView(WebView webView) {
    webViews.add(webView);
    if (currentTempStyleSheet == null) {
      setAndCreateWebViewsStyleSheet(getWebViewStyleSheet());
    } else {
      Platform.runLater(() -> webView.getEngine().setUserStyleSheetLocation(getWebViewStyleSheet()));
    }
  }

  @Override
  public void loadThemes() {
    themesByFolderName.clear();
    themesByFolderName.put(DEFAULT_THEME_NAME, DEFAULT_THEME);
    noCatch(() -> {
      Files.createDirectories(preferencesService.getThemesDirectory());
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(preferencesService.getThemesDirectory())) {
        directoryStream.forEach(this::addThemeDirectory);
      }
    });
  }

  @Override
  public Collection<Theme> getAvailableThemes() {
    return new ArrayList<>(themesByFolderName.values());
  }

  @Override
  public ReadOnlyObjectProperty<Theme> currentThemeProperty() {
    return currentTheme;
  }

  @Override
  public <T extends Controller<?>> T loadFxml(String relativePath) {
    FXMLLoader loader = new FXMLLoader();
    loader.setControllerFactory(applicationContext::getBean);
    loader.setLocation(getThemeFileUrl(relativePath));
    loader.setResources(resources);
    noCatch((NoCatchRunnable) loader::load);
    return loader.getController();
  }

  private Path getThemeDirectory(Theme theme) {
    return preferencesService.getThemesDirectory().resolve(folderNamesByTheme.get(theme));
  }

  private String getWebViewStyleSheet() {
    return getThemeFileUrl(WEBVIEW_CSS_FILE).toString();
  }

  private void setAndCreateWebViewsStyleSheet(String styleSheetUrl) {
    // Always copy to a new file since WebView locks the loaded one
    Path stylesheetsCacheDirectory = preferencesService.getCacheStylesheetsDirectory();

    noCatch(() -> {
      Files.createDirectories(stylesheetsCacheDirectory);

      Path newTempStyleSheet = Files.createTempFile(stylesheetsCacheDirectory, "style-webview", ".css");

      try (InputStream inputStream = new URL(styleSheetUrl).openStream()) {
        Files.copy(inputStream, newTempStyleSheet, StandardCopyOption.REPLACE_EXISTING);
      }
      String newStyleSheetUrl = newTempStyleSheet.toUri().toURL().toString();
      webViews.forEach(webView -> Platform.runLater(() -> webView.getEngine().setUserStyleSheetLocation(newStyleSheetUrl)));
      logger.debug("{} created and applied to all web views", newTempStyleSheet.getFileName());
      if (currentTempStyleSheet != null) {
        Files.delete(currentTempStyleSheet);
      }
      currentTempStyleSheet = newTempStyleSheet;
    });
  }
}
