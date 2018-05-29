package com.faforever.client.theme;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
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
import lombok.SneakyThrows;
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
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
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
import java.util.Objects;
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
public class UiService {

  public static final String UNKNOWN_MAP_IMAGE = "theme/images/unknown_map.png";
  //TODO: Create Images for News Categories
  public static final String SERVER_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String LADDER_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String TOURNAMENT_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String FA_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String LOBBY_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String BALANCE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String WEBSITE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String CAST_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String PODCAST_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String FEATURED_MOD_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String DEVELOPMENT_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String DEFAULT_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  public static final String STYLE_CSS = "theme/style.css";
  public static final String WEBVIEW_CSS_FILE = "theme/style-webview.css";
  public static final String DEFAULT_ACHIEVEMENT_IMAGE = "theme/images/default_achievement.png";
  public static final String MENTION_SOUND = "theme/sounds/mention.mp3";
  public static final String CSS_CLASS_ICON = "icon";
  public static final String LADDER_1V1_IMAGE = "theme/images/ranked1v1_notification.png";
  public static final String CHAT_CONTAINER = "theme/chat/chat_container.html";
  public static final String CHAT_SECTION_EXTENDED = "theme/chat/extended/chat_section.html";
  public static final String CHAT_SECTION_COMPACT = "theme/chat/compact/chat_section.html";
  public static final String CHAT_TEXT_EXTENDED = "theme/chat/extended/chat_text.html";
  public static final String CHAT_TEXT_COMPACT = "theme/chat/compact/chat_text.html";

  public static Theme DEFAULT_THEME = new Theme() {
    {
      setAuthor("Downlord");
      setCompatibilityVersion(1);
      setDisplayName("Default");
      setThemeVersion("1.0");
    }
  };

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * This value needs to be updated whenever theme-breaking changes were made to the client.
   */
  private static final int THEME_VERSION = 1;
  private static final String METADATA_FILE_NAME = "theme.properties";
  private final Set<Scene> scenes;
  private final Set<WeakReference<WebView>> webViews;

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
  public UiService(PreferencesService preferencesService, ThreadPoolExecutor threadPoolExecutor,
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
  void postConstruct() throws IOException {
    resources = new MessageSourceResourceBundle(messageSource, i18n.getUserSpecificLocale());
    Path themesDirectory = preferencesService.getThemesDirectory();
    startWatchService(themesDirectory);
    deleteStylesheetsCacheDirectory();
    loadThemes();

    String storedTheme = preferencesService.getPreferences().getThemeName();
    setTheme(themesByFolderName.get(storedTheme));

    loadWebViewsStyleSheet(getWebViewStyleSheet());
  }

  private void deleteStylesheetsCacheDirectory() throws IOException {
    Path cacheStylesheetsDirectory = preferencesService.getCacheStylesheetsDirectory();
    if (Files.exists(cacheStylesheetsDirectory)) {
      deleteRecursively(cacheStylesheetsDirectory);
    }
  }

  private void startWatchService(Path themesDirectory) throws IOException {
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
  void preDestroy() throws IOException {
    IOUtils.closeQuietly(watchService);
    deleteStylesheetsCacheDirectory();
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

  private void onWatchEvent(WatchKey key) {
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
    String[] styleSheets = getStylesheets();

    logger.debug("Changes detected, reloading stylesheets: {}", (Object[]) styleSheets);
    scenes.forEach(scene -> setSceneStyleSheet(scene, styleSheets));
    loadWebViewsStyleSheet(getWebViewStyleSheet());
  }

  private void setSceneStyleSheet(Scene scene, String[] styleSheets) {
    Platform.runLater(() -> scene.getStylesheets().setAll(styleSheets));
  }

  private String getSceneStyleSheet() {
    return getThemeFile(STYLE_CSS);
  }


  public String getThemeFile(String relativeFile) {
    String strippedRelativeFile = relativeFile.replace("theme/", "");
    Path externalFile = getThemeDirectory(currentTheme.get()).resolve(strippedRelativeFile);
    if (Files.notExists(externalFile)) {
      return noCatch(() -> new ClassPathResource("/" + relativeFile).getURL().toString());
    }
    return noCatch(() -> externalFile.toUri().toURL().toString());
  }

  /**
   * Loads an image from the current theme.
   */
  @Cacheable(CacheNames.THEME_IMAGES)
  public Image getThemeImage(String relativeImage) {
    return new Image(getThemeFile(relativeImage), true);
  }


  public URL getThemeFileUrl(String relativeFile) {
    String themeFile = getThemeFile(relativeFile);
    if (themeFile.startsWith("file:") || themeFile.startsWith("jar:")) {
      return noCatch(() -> new URL(themeFile));
    }
    return noCatch(() -> new ClassPathResource(getThemeFile(relativeFile)).getURL());
  }


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

  /**
   * Unregisters a scene so it's no longer updated when the theme (or its CSS) changes.
   */
  public void unregisterScene(Scene scene) {
    scenes.remove(scene);
  }

  /**
   * Registers a scene against the theme service so it can be updated whenever the theme (or its CSS) changes.
   */
  public void registerScene(Scene scene) {
    scenes.add(scene);

    JavaFxUtil.addListener(scene.getWindow().showingProperty(), (observable, oldValue, newValue) -> {
      if (!newValue) {
        unregisterScene(scene);
      } else {
        registerScene(scene);
      }
    });
    scene.getStylesheets().setAll(getStylesheets());
  }


  public String[] getStylesheets() {
    return new String[]{
        UiService.class.getResource("/css/jfoenix-fonts.css").toExternalForm(),
        UiService.class.getResource("/css/jfoenix-design.css").toExternalForm(),
        getThemeFile("theme/jfoenix.css"),
        getSceneStyleSheet()
    };
  }

  /**
   * Registers a WebView against the theme service so it can be updated whenever the theme changes.
   */
  public void registerWebView(WebView webView) {
    webViews.add(new WeakReference<>(webView));
    webView.getEngine().setUserStyleSheetLocation(getWebViewStyleSheet());
  }

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


  public Collection<Theme> getAvailableThemes() {
    return new ArrayList<>(themesByFolderName.values());
  }


  public ReadOnlyObjectProperty<Theme> currentThemeProperty() {
    return currentTheme;
  }

  /**
   * Loads an FXML file and returns its controller instance. The controller instance is retrieved from the application
   * context, so its scope (which should always be "prototype") depends on the bean definition.
   */
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

  @SneakyThrows
  private void loadWebViewsStyleSheet(String styleSheetUrl) {
    // Always copy to a new file since WebView locks the loaded one
    Path stylesheetsCacheDirectory = preferencesService.getCacheStylesheetsDirectory();

    Files.createDirectories(stylesheetsCacheDirectory);

    Path newTempStyleSheet = Files.createTempFile(stylesheetsCacheDirectory, "style-webview", ".css");

    try (InputStream inputStream = new URL(styleSheetUrl).openStream()) {
      Files.copy(inputStream, newTempStyleSheet, StandardCopyOption.REPLACE_EXISTING);
    }
    if (currentTempStyleSheet != null) {
      Files.delete(currentTempStyleSheet);
    }
    currentTempStyleSheet = newTempStyleSheet;

    webViews.removeIf(reference -> reference.get() != null);
    webViews.stream()
        .map(Reference::get)
        .filter(Objects::nonNull)
        .forEach(webView -> Platform.runLater(
            () -> webView.getEngine().setUserStyleSheetLocation(noCatch(() -> currentTempStyleSheet.toUri().toURL()).toString())));
    logger.debug("{} created and applied to all web views", newTempStyleSheet.getFileName());
  }
}
