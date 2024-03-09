package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePrefsService;
import com.faforever.client.i18n.I18n;
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
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Mod;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.list;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.stream.Collectors.toCollection;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
// TODO divide and conquer
public class ModService implements InitializingBean, DisposableBean {

  private final FafApiAccessor fafApiAccessor;
  private final GamePrefsService gamePrefsService;
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final AssetService assetService;
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

  private final Map<Path, ModVersion> pathToMod = new HashMap<>();
  private final ObservableMap<String, ModVersion> modsByUid = FXCollections.observableHashMap();
  @Getter
  private final ObservableList<ModVersion> installedMods = JavaFxUtil.attachListToMap(
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
                           List.of(new Action(i18n.get("corruptedMods.show"), () -> platformService.reveal(modPath))),
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
                  List.of(new Action(i18n.get("corruptedMods.show"), () -> platformService.reveal(modPath))),
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

  public Mono<Void> downloadIfNecessary(String uid) {
    if (isInstalled(uid)) {
      return Mono.empty();
    }

    return getModVersionByUid(uid).map(modMapper::map)
                                  .flatMap(modVersion -> {
      if (modVersion == null) {
        throw new IllegalArgumentException("Mod with uid %s could not be found".formatted(uid));
      }
                                    return downloadMod(modVersion.downloadUrl(), null, null);
    });
  }

  public Mono<Void> downloadIfNecessary(ModVersion modVersion) {
    return downloadIfNecessary(modVersion, null, null);
  }

  public Mono<Void> downloadIfNecessary(ModVersion modVersion, @Nullable DoubleProperty progressProperty,
                                        @Nullable StringProperty titleProperty) {
    if (isInstalled(modVersion)) {
      return Mono.empty();
    }

    return downloadMod(modVersion.downloadUrl(), progressProperty, titleProperty);
  }

  private Mono<Void> downloadMod(URL url, @Nullable DoubleProperty progressProperty,
                                              @Nullable StringProperty titleProperty) {
    DownloadModTask task = downloadModTaskFactory.getObject();
    task.setUrl(url);
    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getMono();
  }

  public Mono<Void> downloadAndEnableMods(Set<String> modUids) {
    return Mono.when(modUids.stream().map(uid -> downloadIfNecessary(uid).doOnError(throwable -> {
          log.warn("Unable to install mod with uid {}", uid);
    })).toList()).doOnSuccess(ignored -> tryEnableMods(modUids));
  }

  private void tryEnableMods(Set<String> modUids) {
    if (modUids.isEmpty()) {
      return;
    }

    Set<String> installedUiMods = modsByUid.keySet();

    Set<String> activeMods = gamePrefsService.readActiveModUIDs()
                                             .stream()
                                             .filter(installedUiMods::contains)
                                             .collect(Collectors.toSet());

    if (activeMods.containsAll(modUids)) {
      return;
    }

    activeMods.addAll(modUids);

    try {
      gamePrefsService.writeActiveModUIDs(activeMods);
    } catch (IOException e) {
      log.error("Mods could not be enabled", e);
      notificationService.addPersistentWarnNotification(List.of(), "mod.errorUpdatingMods");
    }
  }

  public boolean isInstalled(ModVersion modVersion) {
    return modVersion != null && isInstalled(modVersion.uid());
  }

  public boolean isInstalled(String uid) {
    return modsByUid.containsKey(uid);
  }

  public BooleanExpression isInstalledBinding(ObservableValue<ModVersion> modVersionObservable) {
    return BooleanExpression.booleanExpression(
        Bindings.createBooleanBinding(() -> isInstalled(modVersionObservable.getValue()), modVersionObservable,
                                      installedMods));
  }

  public CompletableFuture<Void> uninstallMod(ModVersion modVersion) {
    UninstallModTask task = uninstallModTaskFactory.getObject();
    task.setMod(modVersion);
    return taskService.submitTask(task).getFuture();
  }

  public Path getPathForMod(ModVersion modVersionToFind) {
    return pathToMod.entrySet()
                    .stream().filter(pathModEntry -> pathModEntry.getValue().uid().equals(modVersionToFind.uid()))
                    .findFirst()
                    .map(Entry::getKey)
                    .orElse(null);
  }

  @NotNull
  public ModVersion extractModInfo(Path modFolder) {
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
  public ModVersion extractModInfo(InputStream inputStream, Path basePath) {
    return modMapper.map(modReader.readModInfo(inputStream, basePath), basePath);
  }

  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = modUploadTaskFactory.getObject();
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public Image loadThumbnail(ModVersion modVersion) {
    return assetService.loadAndCacheImage(modVersion.thumbnailUrl(), Path.of("mods"),
                                          () -> themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE));
  }

  public CompletableFuture<Integer> getFileSize(ModVersion modVersion) {
    return fileSizeReader.getFileSize(modVersion.downloadUrl());
  }

  public Collection<ModVersion> getActivatedSimAndUIMods() throws IOException {
    Set<String> activeMods = gamePrefsService.readActiveModUIDs();
    return installedMods.stream().filter(mod -> activeMods.contains(mod.uid())).collect(Collectors.toSet());
  }

  public void overrideActivatedMods(Collection<ModVersion> modVersions) {
    Set<String> modStates = modVersions.stream().map(ModVersion::uid).collect(Collectors.toSet());
    try {
      gamePrefsService.writeActiveModUIDs(modStates);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to write active mods to game.prefs file");
    }
  }

  private void removeMod(Path path) {
    log.trace("Removing mod: `{}`", path);
    ModVersion modVersion = pathToMod.remove(path);
    if (modVersion != null) {
      modsByUid.remove(modVersion.uid());
    }
  }

  private void addInstalledMod(Path modFolder) {
    ModVersion modVersion = extractModInfo(modFolder);
    pathToMod.put(modFolder, modVersion);
    if (!modsByUid.containsKey(modVersion.uid())) {
      fxApplicationThreadExecutor.execute(() -> modsByUid.put(modVersion.uid(), modVersion));
      log.debug("Added mod from {}", modFolder);
    }
  }

  @Override
  public void destroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }

  public Mono<List<ModVersion>> updateAndActivateModVersions(final Collection<ModVersion> selectedModVersions) {
    if (!preferences.isMapAndModAutoUpdate()) {
      return Mono.just(List.copyOf(selectedModVersions));
    }

    List<Mono<ModVersion>> updatedVersions = selectedModVersions.stream().map(this::updateModIfNecessary).toList();


    return Flux.concat(updatedVersions).collectList().doOnNext(this::overrideActivatedMods);
  }

  private Mono<ModVersion> updateModIfNecessary(ModVersion installedModVersion) {
    return getModVersionByUid(installedModVersion.uid()).map(dto -> modMapper.map(dto.getMod().getLatestVersion()))
                                                           .filter(latestModVersion -> !Objects.equals(latestModVersion,
                                                                                                       installedModVersion))
                                                           .flatMap(latestModVersion -> downloadIfNecessary(
                                                               latestModVersion).thenReturn(latestModVersion))
                                                           .doOnError(throwable -> log.info(
                                                               "Failed fetching info about mod `{}` from the api.",
                                                               installedModVersion.mod().displayName(),
                                                               throwable))
                                                           .onErrorReturn(installedModVersion)
                                                           .defaultIfEmpty(installedModVersion);
  }

  private Mono<com.faforever.commons.api.dto.ModVersion> getModVersionByUid(String uid) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.ModVersion> navigator = ElideNavigator.of(
                                                                                                       com.faforever.commons.api.dto.ModVersion.class)
                                                                                                   .collection()
                                                                                                   .setFilter(
                                                                                                       qBuilder().string(
                                                                                                                     "uid")
                                                                                                                 .eq(uid))
                                                                                                   .pageSize(1)
                                                                                                   .pageNumber(1);
    return fafApiAccessor.getMany(navigator).next();
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public Mono<Tuple2<List<ModVersion>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig, int count,
                                                                          int page) {
    SortConfig sortConfig = searchConfig.sortConfig();
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .addSortingRule(sortConfig.sortProperty(),
                                                                              sortConfig.sortOrder()
                                                                                        .equals(SortOrder.ASC));
    return getModPage(navigator, searchConfig.searchQuery(), count, page);
  }

  @Cacheable(value = CacheNames.MODS, sync = true)
  public Mono<Integer> getRecommendedModPageCount(int count) {
    return getRecommendedModsWithPageCount(count, 1).map(Tuple2::getT2);
  }

  public Mono<Tuple2<List<ModVersion>, Integer>> getRecommendedModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(qBuilder().bool("recommended").isTrue());
    return getModPage(navigator, count, page);
  }

  public Mono<Tuple2<List<ModVersion>, Integer>> getHighestRatedUiModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(
                                                                  qBuilder().string("latestVersion.type").eq("UI"))
                                                              .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public Mono<Tuple2<List<ModVersion>, Integer>> getHighestRatedModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .setFilter(
                                                                  qBuilder().string("latestVersion.type").eq("SIM"))
                                                              .addSortingRule("reviewsSummary.lowerBound", false);
    return getModPage(navigator, count, page);
  }

  public Mono<Tuple2<List<ModVersion>, Integer>> getNewestModsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Mod> navigator = ElideNavigator.of(Mod.class)
                                                              .collection()
                                                              .addSortingRule("latestVersion.createTime", false);
    return getModPage(navigator, count, page);
  }

  private Mono<Tuple2<List<ModVersion>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator, int count,
                                                             int page) {
    return getModPage(navigator, "", count, page);
  }

  private Mono<Tuple2<List<ModVersion>, Integer>> getModPage(ElideNavigatorOnCollection<Mod> navigator,
                                                             String customFilter, int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
                         .map(tuple -> tuple.mapT1(mods -> mods.stream()
                                                               .map(Mod::getLatestVersion).map(modMapper::map)
                                                               .toList()));
  }
}
