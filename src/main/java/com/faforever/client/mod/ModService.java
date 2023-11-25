package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.ModMapper;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.CompletableTask.Priority;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.FeaturedMod;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.mod.ModReader;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.list;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toCollection;
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
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final AssetService assetService;
  private final UiService uiService;
  private final ThemeService themeService;
  private final FileSizeReader fileSizeReader;
  private final ModMapper modMapper;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final Preferences preferences;
  private final ObjectFactory<ModUploadTask> modUploadTaskFactory;
  private final ObjectFactory<DownloadModTask> downloadModTaskFactory;
  private final ObjectFactory<UninstallModTask> uninstallModTaskFactory;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ModReader modReader = new ModReader();

  private final Map<Path, ModVersionBean> pathToMod = new HashMap<>();
  private final ObservableMap<String, ModVersionBean> modsByUid = FXCollections.observableHashMap();
  @Getter
  private final ObservableList<ModVersionBean> installedMods = JavaFxUtil.attachListToMap(
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), modsByUid);
  private final InvalidationListener modDirectoryChangedListener = observable -> tryLoadMods();

  private Thread directoryWatcherThread;

  @Override
  public void afterPropertiesSet() {
    JavaFxUtil.addAndTriggerListener(forgedAlliancePrefs.vaultBaseDirectoryProperty(),
                                     new WeakInvalidationListener(modDirectoryChangedListener));
  }

  private void tryLoadMods() {
    Path modsDirectory = forgedAlliancePrefs.getModsDirectory();
    if (modsDirectory == null) {
      log.warn("Could not load mods: custom mod directory is not set");
      return;
    }

    try {
      createDirectories(modsDirectory);
      Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
      directoryWatcherThread = startDirectoryWatcher(modsDirectory);
    } catch (IOException e) {
      log.warn("Could not start mod directory watcher", e);
    }

    installedMods.clear();
    loadInstalledMods();
  }

  private Thread startDirectoryWatcher(Path modsDirectory) {
    Thread thread = new Thread(() -> {
      try (WatchService watcher = modsDirectory.getFileSystem().newWatchService()) {
        modsDirectory.register(watcher, ENTRY_DELETE, ENTRY_CREATE);
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          key.pollEvents()
             .stream()
             .filter(event -> event.kind() == ENTRY_DELETE || event.kind() == ENTRY_CREATE)
             .forEach(event -> {
               Path modPath = modsDirectory.resolve((Path) event.context());
               if (event.kind() == ENTRY_DELETE) {
                 removeMod(modPath);
               } else if (event.kind() == ENTRY_CREATE) {
                 Mono.just(modPath)
                     .filter(Files::exists)
                     .doOnNext(this::addInstalledMod)
                     .retryWhen(Retry.fixedDelay(30, Duration.ofSeconds(1)).filter(ModLoadException.class::isInstance))
                     .subscribe(null, throwable -> {
                       log.error("Mod could not be read: `{}`", modPath, throwable);
                       notificationService.addPersistentWarnNotification(
                           List.of(new Action(i18n.get("corruptedMods.show"), evt -> platformService.reveal(modPath))),
                           "corruptedModsError.notification", modPath.getFileName());
                     });
               }
             });
          key.reset();
        }
      } catch (IOException e) {
        log.warn("Could not start mods directory watcher on {}", modsDirectory);
      } catch (InterruptedException e) {
        log.debug("Watcher terminated ({})", e.getMessage());
      }
    });
    thread.start();
    return thread;
  }

  private void loadInstalledMods() {
    taskService.submitTask(new CompletableTask<Void>(Priority.LOW) {

      @Override
      protected Void call() {
        updateTitle(i18n.get("modVault.loadingMods"));
        try (Stream<Path> customModsDirectory = list(forgedAlliancePrefs.getModsDirectory())) {
          List<Path> modPaths = new ArrayList<>();
          customModsDirectory.collect(toCollection(() -> modPaths));

          long totalMods = modPaths.size();
          long modsRead = 0;
          for (Path modPath : modPaths) {
            updateProgress(++modsRead, totalMods);
            try {
              addInstalledMod(modPath);
            } catch (Exception e) {
              log.warn("Corrupt mod: `{}`", modPath, e);

              notificationService.addPersistentWarnNotification(
                  List.of(new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(modPath))),
                  "corruptedModsError.notification", modPath.getFileName());
            }
          }
        } catch (IOException e) {
          log.error("Mods could not be read from: `{}`", forgedAlliancePrefs.getModsDirectory(), e);
        }
        return null;
      }
    });
  }

  public CompletableFuture<Void> downloadAndInstallMod(String uid) {
    return getModVersionByUid(uid).thenCompose(potentialModVersion -> {
      ModVersionBean modVersion = potentialModVersion.orElseThrow(
          () -> new IllegalArgumentException("Mod could not be found"));
      return downloadAndInstallMod(modVersion, null, null);
    }).exceptionally(throwable -> {
      log.error("Mod could not be installed", throwable);
      return null;
    });
  }

  public CompletableFuture<Void> downloadAndInstallMod(URL url) {
    return downloadAndInstallMod(url, null, null);
  }

  public CompletableFuture<Void> downloadAndInstallMod(URL url, @Nullable DoubleProperty progressProperty,
                                                       @Nullable StringProperty titleProperty) {
    DownloadModTask task = downloadModTaskFactory.getObject();
    task.setUrl(url);
    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getFuture();
  }

  public CompletableFuture<Void> downloadAndInstallMod(ModVersionBean modVersion,
                                                       @Nullable DoubleProperty progressProperty,
                                                       StringProperty titleProperty) {
    return downloadAndInstallMod(modVersion.getDownloadUrl(), progressProperty, titleProperty);
  }

  public void enableSimMods(Set<String> simMods) throws IOException {
    Set<String> installedUiMods = modsByUid.keySet();

    Set<String> activeMods = readActiveMods().stream().filter(installedUiMods::contains).collect(Collectors.toSet());

    activeMods.addAll(simMods);

    writeActiveMods(activeMods);
  }

  public boolean isInstalled(ModVersionBean modVersion) {
    return modVersion != null && isInstalled(modVersion.getUid());
  }

  public boolean isInstalled(String uid) {
    return modsByUid.containsKey(uid);
  }

  public BooleanExpression isInstalledBinding(ObservableValue<ModVersionBean> modVersionObservable) {
    return BooleanExpression.booleanExpression(
        Bindings.createBooleanBinding(() -> isInstalled(modVersionObservable.getValue()), modVersionObservable,
                                      installedMods));
  }

  public CompletableFuture<Void> uninstallMod(ModVersionBean modVersion) {
    UninstallModTask task = uninstallModTaskFactory.getObject();
    task.setMod(modVersion);
    return taskService.submitTask(task).getFuture();
  }

  public Path getPathForMod(ModVersionBean modVersionToFind) {
    return pathToMod.entrySet()
                    .stream()
                    .filter(pathModEntry -> pathModEntry.getValue().getUid().equals(modVersionToFind.getUid()))
                    .findFirst()
                    .map(Entry::getKey)
                    .orElse(null);
  }

  @NotNull
  public ModVersionBean extractModInfo(Path modFolder) {
    Path modInfoLua = modFolder.resolve("mod_info.lua");
    if (Files.notExists(modInfoLua)) {
      throw new ModLoadException("Missing mod_info.lua in: " + modFolder.toAbsolutePath(), null, "mod.load.noModInfo",
                                 modFolder.toAbsolutePath());
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLua)) {
      return extractModInfo(inputStream, modFolder);
    } catch (IOException e) {
      throw new ModLoadException("IO error loading: " + modFolder.toAbsolutePath(), null, "mod.load.ioError",
                                 modFolder.toAbsolutePath());
    }
  }

  @NotNull
  public ModVersionBean extractModInfo(InputStream inputStream, Path basePath) {
    return modMapper.map(modReader.readModInfo(inputStream, basePath), basePath);
  }

  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = modUploadTaskFactory.getObject();
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public Image loadThumbnail(ModVersionBean modVersion) {
    return assetService.loadAndCacheImage(modVersion.getThumbnailUrl(), Path.of("mods"),
                                          () -> themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE));
  }

  @Async
  public CompletableFuture<Integer> getFileSize(ModVersionBean modVersion) {
    return fileSizeReader.getFileSize(modVersion.getDownloadUrl());
  }

  public Collection<ModVersionBean> getActivatedSimAndUIMods() throws IOException {
    Set<String> activeMods = readActiveMods();
    return installedMods.stream().filter(mod -> activeMods.contains(mod.getUid())).collect(Collectors.toSet());
  }

  public void overrideActivatedMods(Collection<ModVersionBean> modVersions) {
    Set<String> modStates = modVersions.stream().map(ModVersionBean::getUid).collect(Collectors.toSet());
    writeActiveMods(modStates);
  }

  private String readPreferencesFile(Path preferencesFile) throws IOException {
    Map<String, Charset> availableCharsets = Charset.availableCharsets();
    String preferencesContent = null;
    for (Charset charset : availableCharsets.values()) {
      CharsetDecoder decoder = charset.newDecoder();
      decoder.onMalformedInput(CodingErrorAction.REPORT);
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
      log.info("Trying to read preferences file with charset: " + charset.displayName());
      try {
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(preferencesFile));
        CharBuffer charBuffer = decoder.decode(buffer);
        preferencesContent = charBuffer.toString();
        log.info("Successfully read preferences file with charset: " + charset.displayName());
        break;
      } catch (CharacterCodingException e) {
        log.info("Failed to read preferences file with charset: " + charset.displayName());
        // Continue and try a different character set
      } catch (IOException e) {
        log.error("An IOException was thrown while trying to read the preferences file", e);
      } catch (Exception e) {
        // Handle all other exceptions
        log.error("An unexpected error occurred while reading the preferences file", e);
        throw new RuntimeException(e);
      }
    }
    if (preferencesContent == null) {
      throw new AssetLoadException("Could not read preferences file", null, "file.errorReadingPreferences");
    }
    return preferencesContent;
  }

  private Set<String> readActiveMods() throws IOException {
    Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
    Set<String> activeMods = new HashSet<>();
    String preferencesContent = readPreferencesFile(preferencesFile);
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      Matcher activeModMatcher = ACTIVE_MOD_PATTERN.matcher(matcher.group(0));
      while (activeModMatcher.find()) {
        String modUid = activeModMatcher.group(1);
        if (Boolean.parseBoolean(activeModMatcher.group(2))) {
          activeMods.add(modUid);
        }
      }
    }
    return activeMods;
  }

  private void writeActiveMods(Set<String> activeMods) {
    Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
    try {
      String preferencesContent = readPreferencesFile(preferencesFile);
      String currentActiveModsContent = null;
      Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
      if (matcher.find()) {
        currentActiveModsContent = matcher.group(0);
      }

      String newActiveModsContent = "active_mods = {\n%s\n}".formatted(
          activeMods.stream().map("    ['%s'] = true"::formatted).collect(Collectors.joining(",\n")));

      if (currentActiveModsContent != null) {
        preferencesContent = preferencesContent.replace(currentActiveModsContent, newActiveModsContent);
      } else {
        preferencesContent += newActiveModsContent;
      }

      Files.writeString(preferencesFile, preferencesContent);
    } catch (IOException e) {
      throw new AssetLoadException("Could not update mod state", e, "mod.errorUpdatingMods");
    }
  }

  private void removeMod(Path path) {
    log.trace("Removing mod: `{}`", path);
    ModVersionBean modVersion = pathToMod.remove(path);
    if (modVersion != null) {
      modsByUid.remove(modVersion.getUid());
    }
  }

  private void addInstalledMod(Path modFolder) {
    ModVersionBean modVersion = extractModInfo(modFolder);
    pathToMod.put(modFolder, modVersion);
    if (!modsByUid.containsKey(modVersion.getUid())) {
      fxApplicationThreadExecutor.execute(() -> modsByUid.put(modVersion.getUid(), modVersion));
      log.debug("Added mod from {}", modFolder);
    }
  }

  @Override
  public void destroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }

  @Async
  public CompletableFuture<Collection<ModVersionBean>> updateAndActivateModVersions(
      final Collection<ModVersionBean> selectedModVersions) {
    if (!preferences.isMapAndModAutoUpdate()) {
      return CompletableFuture.completedFuture(selectedModVersions);
    }

    final List<ModVersionBean> newlySelectedMods = new ArrayList<>(selectedModVersions);
    selectedModVersions.forEach(installedModVersion -> {
      try {
        Optional<ModVersionBean> modVersionFromApi = getModVersionByUid(installedModVersion.getUid()).get();
        modVersionFromApi.ifPresentOrElse(modVersion -> {
          ModVersionBean latestVersion = modVersion.getMod().getLatestVersion();
          boolean isLatest = latestVersion.getUid().equals(installedModVersion.getUid());
          if (!isLatest) {
            downloadAndInstallMod(latestVersion.getDownloadUrl()).join();
            newlySelectedMods.remove(installedModVersion);
            newlySelectedMods.add(latestVersion);
          }
        }, () -> log.info("Could not find mod `{}` `{}`", installedModVersion.getMod().getDisplayName(),
                          installedModVersion.getUid()));
      } catch (Exception e) {
        log.info("Failed fetching info about mod from the api.", e);
      }
    });
    overrideActivatedMods(newlySelectedMods);
    return completedFuture(newlySelectedMods);
  }

  private CompletableFuture<Optional<ModVersionBean>> getModVersionByUid(String uid) {
    ElideNavigatorOnCollection<ModVersion> navigator = ElideNavigator.of(ModVersion.class)
                                                                     .collection()
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
  public Mono<FeaturedModBean> getFeaturedMod(String technicalName) {
    ElideNavigatorOnCollection<FeaturedMod> navigator = ElideNavigator.of(FeaturedMod.class)
                                                                      .collection()
                                                                      .setFilter(qBuilder().string("technicalName")
                                                                                           .eq(technicalName))
                                                                      .addSortingRule("order", true)
                                                                      .pageSize(1);
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .switchIfEmpty(
                             Mono.error(new IllegalArgumentException("Not a valid featured mod: " + technicalName)))
                         .map(dto -> modMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
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
  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig,
                                                                                           int count, int page) {
    SortConfig sortConfig = searchConfig.sortConfig();
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .addSortingRule(sortConfig.sortProperty(),
                                                                              sortConfig.sortOrder()
                                                                                        .equals(SortOrder.ASC));
    return getModPage(navigator, searchConfig.searchQuery(), count, page);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public CompletableFuture<Integer> getRecommendedModPageCount(int count) {
    return getRecommendedModsWithPageCount(count, 1).thenApply(Tuple2::getT2);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getRecommendedModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(qBuilder().bool("recommended").isTrue());
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getHighestRatedUiModsWithPageCount(int count,
                                                                                                     int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(
                                                                  qBuilder().string("latestVersion.type").eq("UI"))
                                                              .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getHighestRatedModsWithPageCount(int count,
                                                                                                   int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(
                                                                  qBuilder().string("latestVersion.type").eq("SIM"))
                                                              .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getNewestModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .addSortingRule("latestVersion.createTime", false);
    return getModPage(navigator, count, page);
  }

  private CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator,
                                                                              int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator)
                         .map(tuple -> tuple.mapT1(mods -> mods.stream()
                                                               .map(Mod::getLatestVersion)
                                                               .map(dto -> modMapper.map(dto,
                                                                                         new CycleAvoidingMappingContext()))
                                                               .collect(toList())))
                         .toFuture();
  }

  private CompletableFuture<Tuple2<List<ModVersionBean>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator,
                                                                              String customFilter, int count,
                                                                              int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
                         .map(tuple -> tuple.mapT1(mods -> mods.stream()
                                                               .map(Mod::getLatestVersion)
                                                               .map(dto -> modMapper.map(dto,
                                                                                         new CycleAvoidingMappingContext()))
                                                               .collect(toList())))
                         .toFuture();
  }
}
