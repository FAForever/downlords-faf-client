package com.faforever.client.mod;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateErrorNotificationEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.commons.mod.ModLoadException;
import com.faforever.commons.mod.ModReader;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Lazy
@Service
public class ModServiceImpl implements ModService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL);
  private static final Pattern ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL);

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final I18n i18n;
  private final PlatformService platformService;
  private final AssetService assetService;
  private final ModReader modReader;

  private Path modsDirectory;
  private Map<Path, Mod> pathToMod;
  private ObservableList<Mod> installedMods;
  private ObservableList<Mod> readOnlyInstalledMods;
  private Thread directoryWatcherThread;

  @Inject
  // TODO divide and conquer
  public ModServiceImpl(TaskService taskService, FafService fafService, PreferencesService preferencesService,
                        ApplicationContext applicationContext, I18n i18n,
                        PlatformService platformService, AssetService assetService) {
    pathToMod = new HashMap<>();
    modReader = new ModReader();
    installedMods = FXCollections.observableArrayList();
    readOnlyInstalledMods = FXCollections.unmodifiableObservableList(installedMods);
    this.taskService = taskService;
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.applicationContext = applicationContext;
    this.i18n = i18n;
    this.platformService = platformService;
    this.assetService = assetService;
  }

  @PostConstruct
  void postConstruct() throws IOException {
    modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
    preferencesService.getPreferences().getForgedAlliance().modsDirectoryProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        onModDirectoryReady();
      }
    });

    if (modsDirectory != null) {
      onModDirectoryReady();
    }
  }

  private void onModDirectoryReady() {
    try {
      createDirectories(modsDirectory);
      directoryWatcherThread = startDirectoryWatcher(modsDirectory);
    } catch (IOException | InterruptedException e) {
      logger.warn("Could not start mod directory watcher", e);
      // TODO notify user
    }
    loadInstalledMods();
  }

  private Thread startDirectoryWatcher(Path modsDirectory) throws IOException, InterruptedException {
    Thread thread = new Thread(() -> noCatch(() -> {
      WatchService watcher = modsDirectory.getFileSystem().newWatchService();
      modsDirectory.register(watcher, ENTRY_DELETE);

      try {
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          key.pollEvents().stream()
              .filter(event -> event.kind() == ENTRY_DELETE)
              .forEach(event -> removeMod(modsDirectory.resolve((Path) event.context())));
          key.reset();
        }
      } catch (InterruptedException e) {
        logger.debug("Watcher terminated ({})", e.getMessage());
      }
    }));
    thread.start();
    return thread;
  }

  @Override
  public void loadInstalledMods() {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory, entry -> Files.isDirectory(entry))) {
      for (Path path : directoryStream) {
        addMod(path);
      }
    } catch (IOException e) {
      logger.warn("Mods could not be read from: " + modsDirectory, e);
    }
  }

  @Override
  public ObservableList<Mod> getInstalledMods() {
    return readOnlyInstalledMods;
  }

  @SneakyThrows
  @Override
  public CompletableFuture<Void> downloadAndInstallMod(String uid) {
    return fafService.getMod(uid).thenAccept(mod -> downloadAndInstallMod(mod, null, null));
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMod(URL url) {
    return downloadAndInstallMod(url, null, null);
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMod(URL url, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    InstallModTask task = applicationContext.getBean(InstallModTask.class);
    task.setUrl(url);
    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getFuture()
        .thenAccept(aVoid -> loadInstalledMods());
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMod(Mod mod, @Nullable DoubleProperty progressProperty, StringProperty titleProperty) {
    return downloadAndInstallMod(mod.getDownloadUrl(), progressProperty, titleProperty);
  }

  @Override
  public Set<String> getInstalledModUids() {
    return getInstalledMods().stream()
        .map(Mod::getUid)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getInstalledUiModsUids() {
    return getInstalledMods().stream()
        .filter(Mod::getUiOnly)
        .map(Mod::getUid)
        .collect(Collectors.toSet());
  }

  @Override
  public void enableSimMods(Set<String> simMods) throws IOException {
    Map<String, Boolean> modStates = readModStates();

    Set<String> installedUiMods = getInstalledUiModsUids();

    for (Map.Entry<String, Boolean> entry : modStates.entrySet()) {
      String uid = entry.getKey();

      if (!installedUiMods.contains(uid)) {
        // Only disable it if it's a sim mod; because it has not been selected
        entry.setValue(false);
      }
    }
    for (String simModUid : simMods) {
      modStates.put(simModUid, true);
    }

    writeModStates(modStates);
  }

  @Override
  public boolean isModInstalled(String uid) {
    return getInstalledModUids().contains(uid);
  }

  @Override
  public CompletableFuture<Void> uninstallMod(Mod mod) {
    UninstallModTask task = applicationContext.getBean(UninstallModTask.class);
    task.setMod(mod);
    return taskService.submitTask(task).getFuture();
  }

  @Override
  public Path getPathForMod(Mod modToFind) {
    return pathToMod.entrySet().stream()
        .filter(pathModEntry -> pathModEntry.getValue().getUid().equals(modToFind.getUid()))
        .findFirst()
        .map(Entry::getKey)
        .orElse(null);
  }

  @Override
  public CompletableFuture<List<Mod>> getAvailableMods() {
    return fafService.getMods();
  }

  @Override
  public CompletableFuture<List<Mod>> getMostDownloadedMods(int count) {
    return getTopElements(Mod.DOWNLOADS_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletableFuture<List<Mod>> getMostLikedMods(int count) {
    return getTopElements(Mod.LIKES_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletableFuture<List<Mod>> getMostPlayedMods(int count) {
    return getTopElements(Mod.TIMES_PLAYED_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletableFuture<List<Mod>> getNewestMods(int count) {
    return getTopElements(Mod.PUBLISH_DATE_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletableFuture<List<Mod>> getMostLikedUiMods(int count) {
    return getAvailableMods().thenApply(mods -> mods.stream()
        .filter(Mod::getUiOnly)
        .sorted(Mod.LIKES_COMPARATOR.reversed())
        .limit(count)
        .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<Mod>> lookupMod(String string, int maxResults) {
    // FIXME remove
    return CompletableFuture.completedFuture(emptyList());
  }

  @NotNull
  @Override
  @SneakyThrows
  public Mod extractModInfo(Path path) {
    Path modInfoLua = path.resolve("mod_info.lua");
    logger.debug("Reading mod {}", path);
    if (Files.notExists(modInfoLua)) {
      throw new ModLoadException("Missing mod_info.lua in: " + path.toAbsolutePath());
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLua)) {
      return extractModInfo(inputStream, path);
    }
  }

  @NotNull
  @Override
  public Mod extractModInfo(InputStream inputStream, Path basePath) {
    return Mod.fromModInfo(modReader.readModInfo(inputStream, basePath), basePath);
  }

  @Override
  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = applicationContext.getBean(ModUploadTask.class);
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  @Override
  @Cacheable(value = CacheNames.MOD_THUMBNAIL, unless = "#result == null")
  public Image loadThumbnail(Mod mod) {
    URL url = mod.getThumbnailUrl();
    return assetService.loadAndCacheImage(url, Paths.get("mods"), () -> IdenticonUtil.createIdenticon(mod.getName()));
  }

  @Override
  public void evictModsCache() {
    fafService.evictModsCache();
  }

  @Override
  @SneakyThrows
  public long getModSize(Mod mod) {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) mod.getDownloadUrl().openConnection();
      conn.setRequestMethod(HttpMethod.HEAD.name());
      return conn.getContentLength();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  @Override
  public ComparableVersion readModVersion(Path modDirectory) {
    return extractModInfo(modDirectory).getVersion();
  }

  @Override
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }

  @Override
  public CompletableFuture<FeaturedMod> getFeaturedMod(String featuredMod) {
    return getFeaturedMods().thenCompose(featuredModBeans -> completedFuture(featuredModBeans.stream()
        .filter(mod -> featuredMod.equals(mod.getTechnicalName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Not a valid featured mod: " + featuredMod))
    ));
  }

  @Override
  public List<Mod> getActivatedSimAndUIMods() throws IOException {
    Map<String, Boolean> modStates = readModStates();
    return getInstalledMods().parallelStream()
        .filter(mod -> modStates.containsKey(mod.getUid()) && modStates.get(mod.getUid()))
        .collect(Collectors.toList());
  }

  @Override
  public void overrideActivatedMods(List<Mod> mods) throws IOException {
    Map<String, Boolean> modStates = mods.parallelStream().collect(Collectors.toMap(Mod::getUid, o -> true));
    writeModStates(modStates);
  }

  private CompletableFuture<List<Mod>> getTopElements(Comparator<? super Mod> comparator, int count) {
    return getAvailableMods().thenApply(mods -> mods.stream()
        .sorted(comparator)
        .limit(count)
        .collect(Collectors.toList()));
  }

  private Map<String, Boolean> readModStates() throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    Map<String, Boolean> mods = new HashMap<>();

    String preferencesContent = new String(Files.readAllBytes(preferencesFile), US_ASCII);
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      Matcher activeModMatcher = ACTIVE_MOD_PATTERN.matcher(matcher.group(0));
      while (activeModMatcher.find()) {
        String modUid = activeModMatcher.group(1);
        boolean enabled = Boolean.parseBoolean(activeModMatcher.group(2));

        mods.put(modUid, enabled);
      }
    }

    return mods;
  }

  private void writeModStates(Map<String, Boolean> modStates) throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    String preferencesContent = new String(Files.readAllBytes(preferencesFile), US_ASCII);

    String currentActiveModsContent = null;
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      currentActiveModsContent = matcher.group(0);
    }

    StringBuilder newActiveModsContentBuilder = new StringBuilder("active_mods = {");

    Iterator<Map.Entry<String, Boolean>> iterator = modStates.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Boolean> entry = iterator.next();
      if (!entry.getValue()) {
        continue;
      }

      newActiveModsContentBuilder.append("\n    ['");
      newActiveModsContentBuilder.append(entry.getKey());
      newActiveModsContentBuilder.append("'] = true");
      if (iterator.hasNext()) {
        newActiveModsContentBuilder.append(",");
      }
    }
    newActiveModsContentBuilder.append("\n}");

    if (currentActiveModsContent != null) {
      preferencesContent = preferencesContent.replace(currentActiveModsContent, newActiveModsContentBuilder);
    } else {
      preferencesContent += newActiveModsContentBuilder.toString();
    }

    Files.write(preferencesFile, preferencesContent.getBytes(US_ASCII));
  }

  private void removeMod(Path path) {
    logger.debug("Removing mod: {}", path);
    installedMods.remove(pathToMod.remove(path));
  }

  private void addMod(Path path) {
    logger.debug("Adding mod: {}", path);
    try {
      Mod mod = extractModInfo(path);
      pathToMod.put(path, mod);
      if (!installedMods.contains(mod)) {
        installedMods.add(mod);
      }
    } catch (ModLoadException e) {
      logger.debug("Corrupt mod: " + path, e);
      applicationContext.publishEvent(new ShowImmediateErrorNotificationEvent(e, "corruptedMods.notification", path.getFileName()));
    }
  }

  @PreDestroy
  private void preDestroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }
}
