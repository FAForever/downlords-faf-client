package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.ModMapper;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.CompletableTask.Priority;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.FeaturedMod;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.mod.ModLoadException;
import com.faforever.commons.mod.ModReader;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
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

import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
// TODO divide and conquer
public class ModService implements InitializingBean, DisposableBean {

  private static final Pattern ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL);
  private static final Pattern ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL);

  private final FafApiAccessor fafApiAccessor;
  private final PreferencesService preferencesService;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final AssetService assetService;
  private final ModMapper modMapper;
  private final ModReader modReader = new ModReader();

  private Path modsDirectory;
  private final Map<Path, ModVersionBean> pathToMod = new HashMap<>();
  private final ObservableList<ModVersionBean> installedModVersions = FXCollections.observableArrayList();
  private final ObservableList<ModVersionBean> readOnlyInstalledModVersions = FXCollections.unmodifiableObservableList(installedModVersions);
  private Thread directoryWatcherThread;

  @Override
  public void afterPropertiesSet() {
    InvalidationListener modDirectoryChangedListener = observable -> {
      modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
      if (modsDirectory != null) {
        installedModVersions.clear();
        onModDirectoryReady();
      }
    };
    taskService.submitTask(new CompletableTask<Void>(Priority.LOW) {
      @Override
      protected Void call() throws Exception {
        updateTitle(i18n.get("modVault.loadingMods"));
        modDirectoryChangedListener.invalidated(preferencesService.getPreferences().getForgedAlliance().modsDirectoryProperty());
        return null;
      }
    });
    JavaFxUtil.addListener(preferencesService.getPreferences().getForgedAlliance().modsDirectoryProperty(), modDirectoryChangedListener);
  }

  private void onModDirectoryReady() {
    try {
      createDirectories(modsDirectory);
      Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
      directoryWatcherThread = startDirectoryWatcher(modsDirectory);
    } catch (IOException e) {
      log.warn("Could not start mod directory watcher", e);
      // TODO notify user
    }
    loadInstalledMods();
  }

  private Thread startDirectoryWatcher(Path modsDirectory) {
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
        log.debug("Watcher terminated ({})", e.getMessage());
      }
    }));
    thread.start();
    return thread;
  }

  public void loadInstalledMods() {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory, Files::isDirectory)) {
      for (Path path : directoryStream) {
        addMod(path);
      }
    } catch (IOException e) {
      log.warn("Mods could not be read from: " + modsDirectory, e);
    }
  }

  public ObservableList<ModVersionBean> getInstalledModVersions() {
    return readOnlyInstalledModVersions;
  }

  @SneakyThrows
  public CompletableFuture<Void> downloadAndInstallMod(String uid) {
    return getModVersionByUid(uid)
        .thenCompose(potentialModVersion -> {
          ModVersionBean modVersion = potentialModVersion.orElseThrow(() -> new IllegalArgumentException("Mod could not be found"));
          return downloadAndInstallMod(modVersion, null, null);
        })
        .exceptionally(throwable -> {
          log.warn("Sim mod could not be installed", throwable);
          return null;
        });
  }

  public CompletableFuture<Void> downloadAndInstallMod(URL url) {
    return downloadAndInstallMod(url, null, null);
  }

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
        .thenRun(this::loadInstalledMods);
  }

  public CompletableFuture<Void> downloadAndInstallMod(ModVersionBean modVersion, @Nullable DoubleProperty progressProperty, StringProperty titleProperty) {
    return downloadAndInstallMod(modVersion.getDownloadUrl(), progressProperty, titleProperty);
  }

  public Set<String> getInstalledModUids() {
    return getInstalledModVersions().stream()
        .map(ModVersionBean::getUid)
        .collect(Collectors.toSet());
  }

  public Set<String> getInstalledUiModsUids() {
    return getInstalledModVersions().stream()
        .filter(mod -> mod.getModType() == ModType.UI)
        .map(ModVersionBean::getUid)
        .collect(Collectors.toSet());
  }

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

  public boolean isModInstalled(String uid) {
    return getInstalledModUids().contains(uid);
  }

  public CompletableFuture<Void> uninstallMod(ModVersionBean modVersion) {
    UninstallModTask task = applicationContext.getBean(UninstallModTask.class);
    task.setModVersion(modVersion);
    return taskService.submitTask(task).getFuture();
  }

  public Path getPathForMod(ModVersionBean modVersionToFind) {
    return pathToMod.entrySet().stream()
        .filter(pathModEntry -> pathModEntry.getValue().getUid().equals(modVersionToFind.getUid()))
        .findFirst()
        .map(Entry::getKey)
        .orElse(null);
  }

  @NotNull
  @SneakyThrows
  public ModVersionBean extractModInfo(Path path) {
    Path modInfoLua = path.resolve("mod_info.lua");
    log.debug("Reading mod {}", path);
    if (Files.notExists(modInfoLua)) {
      throw new ModLoadException("Missing mod_info.lua in: " + path.toAbsolutePath());
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLua)) {
      return extractModInfo(inputStream, path);
    }
  }

  @NotNull
  public ModVersionBean extractModInfo(InputStream inputStream, Path basePath) {
    return modMapper.map(modReader.readModInfo(inputStream, basePath), basePath, new CycleAvoidingMappingContext());
  }

  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = applicationContext.getBean(ModUploadTask.class);
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public Image loadThumbnail(ModVersionBean modVersion) {
    //FIXME: reintroduce correct caching
    URL url = modVersion.getThumbnailUrl();
    return assetService.loadAndCacheImage(url, Paths.get("mods"), () -> IdenticonUtil.createIdenticon(modVersion.getMod().getDisplayName()));
  }

  /**
   * Returns the download size of the specified modVersion in bytes.
   */
  @SneakyThrows
  public long getModSize(ModVersionBean modVersion) {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) modVersion.getDownloadUrl().openConnection();
      conn.setRequestMethod(HttpMethod.HEAD.name());
      return conn.getContentLength();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public List<ModVersionBean> getActivatedSimAndUIMods() throws IOException {
    Map<String, Boolean> modStates = readModStates();
    return getInstalledModVersions().parallelStream()
        .filter(mod -> modStates.containsKey(mod.getUid()) && modStates.get(mod.getUid()))
        .collect(Collectors.toList());
  }

  public void overrideActivatedMods(List<ModVersionBean> modVersions) throws IOException {
    Map<String, Boolean> modStates = modVersions.parallelStream().collect(Collectors.toMap(ModVersionBean::getUid, o -> true));
    writeModStates(modStates);
  }

  private Map<String, Boolean> readModStates() throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    Map<String, Boolean> mods = new HashMap<>();

    String preferencesContent = Files.readString(preferencesFile, US_ASCII);
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
    String preferencesContent = Files.readString(preferencesFile, US_ASCII);

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

    Files.writeString(preferencesFile, preferencesContent, US_ASCII);
  }

  private void removeMod(Path path) {
    log.debug("Removing mod: {}", path);
    installedModVersions.remove(pathToMod.remove(path));
  }

  private void addMod(Path path) {
    try {
      ModVersionBean modVersion = extractModInfo(path);
      pathToMod.put(path, modVersion);
      if (!installedModVersions.contains(modVersion)) {
        installedModVersions.add(modVersion);
      }
    } catch (ModLoadException e) {
      log.debug("Corrupt mod: " + path, e);

      notificationService.addNotification(new PersistentNotification(i18n.get("corruptedMods.notification", path.getFileName()), WARN, singletonList(
          new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(path))
      )));
    } catch (Exception e) {
      log.warn("Skipping mod because of exception during adding of mod: " + path, e);

      notificationService.addNotification(new PersistentNotification(i18n.get("corruptedModsError.notification", path.getFileName()), WARN, singletonList(
          new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(path))
      )));

    }
  }

  @Override
  public void destroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }

  @Async
  @SneakyThrows
  public CompletableFuture<List<ModVersionBean>> updateAndActivateModVersions(final List<ModVersionBean> selectedModVersions) {
    if (!preferencesService.getPreferences().getMapAndModAutoUpdate()) {
      return CompletableFuture.completedFuture(selectedModVersions);
    }

    final List<ModVersionBean> newlySelectedMods = new ArrayList<>(selectedModVersions);
    selectedModVersions.forEach(installedModVersion -> {
      try {
        Optional<ModVersionBean> modVersionFromApi = getModVersionByUid(installedModVersion.getUid()).get();
        if (modVersionFromApi.isPresent()) {
          ModVersionBean latestVersion = modVersionFromApi.get().getMod().getLatestVersion();
          boolean isLatest = latestVersion.getUid().equals(installedModVersion.getUid());
          if (!isLatest) {
            downloadAndInstallMod(latestVersion.getDownloadUrl()).get();
            newlySelectedMods.remove(installedModVersion);
            newlySelectedMods.add(latestVersion);
          }
        } else {
          log.info("Could not find mod '{}' '{}'", installedModVersion.getMod().getDisplayName(), installedModVersion.getUid());
        }
      } catch (Exception e) {
        log.debug("Failed fetching info about mod from the api.", e);
      }
    });
    overrideActivatedMods(newlySelectedMods);
    return completedFuture(newlySelectedMods);
  }

  private CompletableFuture<Optional<ModVersionBean>> getModVersionByUid(String uid) {
    ElideNavigatorOnCollection<ModVersion> navigator = ElideNavigator.of(ModVersion.class).collection()
        .setFilter(qBuilder().string("uid").eq(uid))
        .pageSize(1)
        .pageNumber(1);
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @Cacheable(value = CacheNames.FEATURED_MOD_FILES, sync = true)
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedModBean featuredMod, Integer version) {
    String endpoint = format("/featuredMods/%s/files/%s", featuredMod.getId(),
        Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return fafApiAccessor.getMany(FeaturedModFile.class, endpoint, fafApiAccessor.getMaxPageSize(), java.util.Map.of())
        .collectList()
        .switchIfEmpty(Mono.just(List.of()))
        .toFuture();
  }

  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public CompletableFuture<FeaturedModBean> getFeaturedMod(String technicalName) {
    ElideNavigatorOnCollection<FeaturedMod> navigator = ElideNavigator.of(FeaturedMod.class)
        .collection()
        .setFilter(qBuilder().string("technicalName").eq(technicalName))
        .addSortingRule("order", true)
        .pageSize(1);
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext()))
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Not a valid featured mod: " + technicalName)))
        .toFuture();
  }

  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public CompletableFuture<List<FeaturedModBean>> getFeaturedMods() {
    ElideNavigatorOnCollection<FeaturedMod> navigator = ElideNavigator.of(FeaturedMod.class)
        .collection()
        .setFilter(qBuilder().bool("visible").isTrue())
        .addSortingRule("order", true)
        .pageSize(50);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig, int count, int page) {
    SortConfig sortConfig = searchConfig.getSortConfig();
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class).collection()
        .addSortingRule(sortConfig.getSortProperty(), sortConfig.getSortOrder().equals(SortOrder.ASC));
    return getModPage(navigator, searchConfig.getSearchQuery(), count, page);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public CompletableFuture<Integer> getRecommendedModPageCount(int count) {
    return getRecommendedModsWithPageCount(count, 1).thenApply(Tuple2::getT2);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getRecommendedModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class).collection()
        .setFilter(qBuilder().bool("recommended").isTrue());
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getHighestRatedUiModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class).collection()
        .setFilter(qBuilder().string("latestVersion.type").eq("UI"))
        .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getHighestRatedModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class).collection()
        .setFilter(qBuilder().string("latestVersion.type").eq("SIM"))
        .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getNewestModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class).collection()
        .addSortingRule("latestVersion.createTime", false);
    return getModPage(navigator, count, page);
  }

  private CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator, int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(Mod::getLatestVersion).map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext())).collect(toList())
        ))
        .toFuture();
  }

  private CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator, String customFilter, int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(Mod::getLatestVersion).map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext())).collect(toList())
        ))
        .toFuture();
  }
}
