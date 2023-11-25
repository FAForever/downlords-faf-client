package com.faforever.client.theme;

import ch.micheljung.fxwindow.FxStage;
import ch.micheljung.waitomo.WaitomoTheme;
import com.faforever.client.config.CacheNames;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.ui.dialog.Dialog.DialogTransition;
import com.faforever.client.ui.dialog.DialogLayout;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.faforever.client.preferences.Preferences.DEFAULT_THEME_NAME;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ThemeService implements InitializingBean, DisposableBean {

  public static final String GENERATED_MAP_IMAGE = "theme/images/generatedMapIcon.png";
  public static final String NO_IMAGE_AVAILABLE = "images/no_image_available.png";
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
  public static final String MENTION_SOUND = "theme/sounds/userMentionSound.mp3";
  public static final String CSS_CLASS_ICON = "icon";
  public static final String CHAT_CONTAINER = "theme/chat/chat_container.html";
  public static final String CHAT_SECTION_EXTENDED = "theme/chat/extended/chat_section.html";
  public static final String CHAT_SECTION_COMPACT = "theme/chat/compact/chat_section.html";
  public static final String CHAT_TEXT_EXTENDED = "theme/chat/extended/chat_text.html";
  public static final String CHAT_TEXT_COMPACT = "theme/chat/compact/chat_text.html";
  public static final String CHAT_LIST_STATUS_HOSTING = "theme/images/player_status/host.png";
  public static final String CHAT_LIST_STATUS_LOBBYING = "theme/images/player_status/lobby.png";
  public static final String CHAT_LIST_STATUS_PLAYING = "theme/images/player_status/playing.png";
  public static final String AEON_STYLE_CLASS = "aeon-icon";
  public static final String CYBRAN_STYLE_CLASS = "cybran-icon";
  public static final String SERAPHIM_STYLE_CLASS = "seraphim-icon";
  public static final String UEF_STYLE_CLASS = "uef-icon";
  public static final String RANDOM_FACTION_IMAGE = "/images/factions/random.png";

  public static Theme DEFAULT_THEME = new Theme("Default", "Downlord", 1, "1");

  private static final String METADATA_FILE_NAME = "theme.properties";

  private final ExecutorService executorService;
  private final ApplicationContext applicationContext;
  private final DataPrefs dataPrefs;
  private final Preferences preferences;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final Set<Scene> scenes = Collections.synchronizedSet(new HashSet<>());
  private final Set<WeakReference<WebView>> webViews = new HashSet<>();
  private final ObservableMap<String, Theme> themesByFolderName = FXCollections.observableHashMap();
  private final Map<Theme, String> folderNamesByTheme = new HashMap<>();
  private final Map<Path, WatchKey> watchKeys = new HashMap<>();
  private final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(DEFAULT_THEME);

  private WatchService watchService;
  private Path currentTempStyleSheet;

  @Override
  public void afterPropertiesSet() throws IOException {
    themesByFolderName.addListener((MapChangeListener<String, Theme>) change -> {
      if (change.wasRemoved()) {
        folderNamesByTheme.remove(change.getValueRemoved());
      }
      if (change.wasAdded()) {
        folderNamesByTheme.put(change.getValueAdded(), change.getKey());
      }
    });

    Path themesDirectory = dataPrefs.getThemesDirectory();
    startWatchService(themesDirectory);
    deleteStylesheetsCacheDirectory();
    loadThemes();

    String storedTheme = preferences.getThemeName();
    if (themesByFolderName.containsKey(storedTheme)) {
      setTheme(themesByFolderName.get(storedTheme));
    } else {
      log.warn("Selected theme was not found in folder {}, falling back to default.", storedTheme);
      setTheme(DEFAULT_THEME);
    }

    loadWebViewsStyleSheet(getWebViewStyleSheet());
  }

  private void deleteStylesheetsCacheDirectory() {
    Path cacheStylesheetsDirectory = dataPrefs.getCacheStylesheetsDirectory();
    if (Files.exists(cacheStylesheetsDirectory)) {
      try {
        FileSystemUtils.deleteRecursively(cacheStylesheetsDirectory);
      } catch (IOException e) {
        log.warn("Missing permission to delete style sheets cache directory '{}'", cacheStylesheetsDirectory);
      }
    }
  }

  private void startWatchService(Path themesDirectory) throws IOException {
    watchService = themesDirectory.getFileSystem().newWatchService();
    executorService.execute(() -> {
      try {
        while (!Thread.interrupted()) {
          WatchKey key = watchService.take();
          onWatchEvent(key);
          key.reset();
        }
      } catch (InterruptedException | ClosedWatchServiceException e) {
        log.info("Watcher service terminated");
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
      log.error("Theme could not be read: {}", metadataFile, e);
    }
  }

  private Theme readTheme(Reader reader) throws IOException {
    Properties properties = new Properties();
    properties.load(reader);
    return Theme.fromProperties(properties);
  }

  @Override
  public void destroy() throws IOException {
    IOUtils.closeQuietly(watchService);
    deleteStylesheetsCacheDirectory();
  }

  private void stopWatchingOldThemes() {
    watchKeys.values().forEach(WatchKey::cancel);
    watchKeys.clear();
  }

  /**
   * Watches all contents in the specified theme for changes and reloads the theme if a change is detected.
   */
  private void watchTheme(Theme theme) {
    Path themePath = getThemeDirectory(theme);
    log.info("Watching theme directory for changes: {}", themePath);
    try {
      Files.walkFileTree(themePath, new DirectoryVisitor(path -> watchDirectory(themePath, watchService)));
    } catch (IOException e) {
      throw new AssetLoadException("Unable to walk theme directory " + themePath, e, "theme.couldNotWatch");
    }

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
    try {
      //When replacing a theme file sometimes it is deleted and added again a few milli seconds later.
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.info("Watch thread was interrupted");
    }
    reloadStylesheet();
  }

  private void watchDirectory(Path directory, WatchService watchService) {
    if (watchKeys.containsKey(directory)) {
      return;
    }
    log.info("Watching directory: {}", directory);
    try {
      watchKeys.put(directory, directory.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE));
    } catch (IOException e) {
      throw new AssetLoadException("Unable to watch directory " + directory, e, "theme.couldNotWatch");
    }
  }

  private void reloadStylesheet() {
    String[] styleSheets = getStylesheets();

    log.info("Changes detected, reloading stylesheets: {}", (Object) styleSheets);
    scenes.forEach(scene -> setSceneStyleSheet(scene, styleSheets));
    loadWebViewsStyleSheet(getWebViewStyleSheet());
  }

  private void setSceneStyleSheet(Scene scene, String[] styleSheets) {
    fxApplicationThreadExecutor.execute(() -> scene.getStylesheets().setAll(styleSheets));
  }

  private String getSceneStyleSheet() throws IOException {
    return getThemeFile(STYLE_CSS);
  }


  public String getThemeFile(String relativeFile) throws IOException {
    String strippedRelativeFile = relativeFile.replace("theme/", "");
    Path externalFile = getThemeDirectory(currentTheme.get()).resolve(strippedRelativeFile);
    if (Files.notExists(externalFile)) {
      return new ClassPathResource("/" + relativeFile).getURL().toString();
    }
    return externalFile.toUri().toURL().toString();
  }

  /**
   * Loads an image from the current theme.
   */
  @Cacheable(value = CacheNames.THEME_IMAGES, sync = true)
  public Image getThemeImage(String relativeImage) {
    try {
      return new Image(getThemeFile(relativeImage), true);
    } catch (IOException e) {
      throw new AssetLoadException("Could not load image " + relativeImage, e, "theme.couldNotLoadImage",
                                   relativeImage);
    }
  }

  /**
   * Loads an image with caching.
   */
  @Cacheable(value = CacheNames.IMAGES, sync = true)
  public Image getImage(String relativeImage) {
    return new Image(relativeImage, true);
  }


  @Cacheable(value = CacheNames.THEME_URLS, sync = true)
  public URL getThemeFileUrl(String relativeFile) throws IOException {
    String themeFile = getThemeFile(relativeFile);
    if (themeFile.startsWith("file:") || themeFile.startsWith("jar:")) {
      return new URL(themeFile);
    }
    return new ClassPathResource(getThemeFile(relativeFile)).getURL();
  }


  @CacheEvict({CacheNames.THEME_URLS, CacheNames.THEME_IMAGES})
  public void setTheme(Theme theme) {
    stopWatchingOldThemes();

    if (theme == DEFAULT_THEME) {
      preferences.setThemeName(DEFAULT_THEME_NAME);
    } else {
      watchTheme(theme);
      preferences.setThemeName(getThemeDirectory(theme).getFileName().toString());
    }
    currentTheme.set(theme);
    reloadStylesheet();
  }

  /**
   * Unregisters a scene so it's no longer updated when the theme (or its CSS) changes.
   */
  private void unregisterScene(Scene scene) {
    scenes.remove(scene);
  }

  /**
   * Registers a scene against the theme service so it can be updated whenever the theme (or its CSS) changes.
   */
  private void registerScene(Scene scene) {
    scenes.add(scene);

    scene.windowProperty().subscribe((oldWindow, newWindow) -> {
      if (oldWindow != null) {
        throw new UnsupportedOperationException("Not supposed to happen");
      }
      if (newWindow != null) {
        newWindow.showingProperty().subscribe(newValue -> {
          if (!newValue) {
            unregisterScene(scene);
          } else {
            registerScene(scene);
          }
        });
      }
    });
    scene.getStylesheets().setAll(getStylesheets());
  }

  private String[] getStylesheets() {
    try {
      return new String[]{
          FxStage.BASE_CSS.toExternalForm(),
          FxStage.UNDECORATED_CSS.toExternalForm(),
          WaitomoTheme.WAITOMO_CSS.toExternalForm(),
          getThemeFile("theme/colors.css"),
          getThemeFile("theme/icons.css"),
          getSceneStyleSheet(),
          getThemeFile("theme/style_extension.css")
      };
    } catch (IOException e) {
      throw new AssetLoadException("Could not retrieve stylesheets", e, "theme.stylesheets.couldNotGet");
    }
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
    try {
      Files.createDirectories(dataPrefs.getThemesDirectory());
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataPrefs.getThemesDirectory())) {
        directoryStream.forEach(this::addThemeDirectory);
      }
    } catch (IOException e) {
      throw new AssetLoadException("Could not load themes from " + dataPrefs.getThemesDirectory(), e,
                                   "theme.couldNotLoad", e.getLocalizedMessage());
    }
  }

  public List<Theme> getAvailableThemes() {
    return List.copyOf(themesByFolderName.values());
  }

  public Theme getCurrentTheme() {
    return currentTheme.get();
  }

  public ReadOnlyObjectProperty<Theme> currentThemeProperty() {
    return currentTheme;
  }

  private Path getThemeDirectory(Theme theme) {
    return dataPrefs.getThemesDirectory().resolve(folderNamesByTheme.get(theme));
  }

  private String getWebViewStyleSheet() {
    try {
      return getThemeFileUrl(WEBVIEW_CSS_FILE).toString();
    } catch (IOException e) {
      throw new AssetLoadException("Could not get webview stylesheet", e, "theme.couldNotLoad",
                                   e.getLocalizedMessage());
    }

  }

  private void loadWebViewsStyleSheet(String styleSheetUrl) {
    try {
      // Always copy to a new file since WebView locks the loaded one
      Path stylesheetsCacheDirectory = dataPrefs.getCacheStylesheetsDirectory();

      Files.createDirectories(stylesheetsCacheDirectory);

      Path newTempStyleSheet = Files.createTempFile(stylesheetsCacheDirectory, "style-webview", ".css");

      try (InputStream inputStream = new URL(styleSheetUrl).openStream()) {
        Files.copy(inputStream, newTempStyleSheet, StandardCopyOption.REPLACE_EXISTING);
      }
      if (currentTempStyleSheet != null) {
        Files.delete(currentTempStyleSheet);
      }
      currentTempStyleSheet = newTempStyleSheet;
      String urlString = currentTempStyleSheet.toUri().toURL().toString();

      webViews.removeIf(reference -> reference.get() != null);
      webViews.stream()
              .map(Reference::get)
              .filter(Objects::nonNull)
              .forEach(webView -> fxApplicationThreadExecutor.execute(
                  () -> {
                    webView.getEngine().setUserStyleSheetLocation(urlString);
                  }));
      log.info("{} created and applied to all web views", newTempStyleSheet.getFileName());
    } catch (IOException e) {
      throw new AssetLoadException("Could not load webview stylesheet", e, "theme.webview.stylesheet.couldNotLoad",
                                   styleSheetUrl);
    }
  }

  public Scene createScene(Parent root) {
    Scene scene = new Scene(root);
    registerScene(root.getScene());
    return scene;
  }

  public Dialog showInDialog(StackPane parent, Node content) {
    return showInDialog(parent, content, null);
  }

  public Dialog showInDialog(StackPane parent, Node content, String title) {
    DialogLayout dialogLayout = new DialogLayout();
    if (title != null) {
      dialogLayout.setHeading(new Label(title));
    }
    dialogLayout.setBody(content);

    Dialog dialog = new Dialog();
    dialog.setContent(dialogLayout);
    dialog.setTransitionType(DialogTransition.TOP);

    parent.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        dialog.close();
      }
    });

    dialog.show(parent);
    return dialog;
  }

  public boolean doesThemeNeedRestart(Theme theme) {
    if (theme.equals(DEFAULT_THEME)) {
      return true;
    }
    try (Stream<Path> stream = Files.list(getThemeDirectory(theme))) {
      return stream.anyMatch(
          path -> Files.isRegularFile(path) && !path.endsWith(".css") && !path.endsWith(".properties"));
    } catch (IOException e) {
      throw new AssetLoadException("Could not load theme from " + theme.getDisplayName(), e,
                                   "theme.directory.readError", theme.getDisplayName());
    }
  }
}
