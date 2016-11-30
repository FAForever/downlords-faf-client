package com.faforever.client.mod;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.patch.MountPoint;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.IdenticonUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.client.util.LuaUtil.load;
import static com.faforever.client.util.LuaUtil.loadFile;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class ModServiceImpl implements ModService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL);
  private static final Pattern ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL);
  private static final Lock LOOKUP_LOCK = new ReentrantLock();

  @Resource
  FafService fafService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  TaskService taskService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  Analyzer analyzer;
  @Resource
  Directory directory;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  PlatformService platformService;
  @Resource
  AssetService assetService;

  private Path modsDirectory;
  private Map<Path, ModInfoBean> pathToMod;
  private ObservableList<ModInfoBean> installedMods;
  private ObservableList<ModInfoBean> readOnlyInstalledMods;
  private AnalyzingInfixSuggester suggester;

  public ModServiceImpl() {
    pathToMod = new HashMap<>();
    installedMods = FXCollections.observableArrayList();
    readOnlyInstalledMods = FXCollections.unmodifiableObservableList(installedMods);
  }

  private static Path extractIconPath(Path path, LuaValue luaValue) {
    String icon = luaValue.get("icon").toString();
    if ("nil".equals(icon) || StringUtils.isEmpty(icon)) {
      return null;
    }

    if (icon.startsWith("/")) {
      icon = icon.substring(1);
    }

    Path iconPath = Paths.get(icon);
    // FIXME try-catch until I know exactly what's the value that causes #228
    try {
      // mods/BlackOpsUnleashed/icons/yoda_icon.bmp -> icons/yoda_icon.bmp
      iconPath = iconPath.subpath(2, iconPath.getNameCount());
    } catch (IllegalArgumentException e) {
      logger.warn("Can't load icon for mod: {}, icon path: {}", path, iconPath);
      return null;
    }

    return path.resolve(iconPath);
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

    suggester = new AnalyzingInfixSuggester(directory, analyzer);
  }

  private void onModDirectoryReady() {
    try {
      createDirectories(modsDirectory);
      startDirectoryWatcher(modsDirectory);
    } catch (IOException | InterruptedException e) {
      logger.warn("Could not start mod directory watcher", e);
      // TODO notify user
    }
    loadInstalledMods();
  }

  private void startDirectoryWatcher(Path modsDirectory) throws IOException, InterruptedException {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        WatchService watcher = modsDirectory.getFileSystem().newWatchService();
        modsDirectory.register(watcher, ENTRY_DELETE);

        //noinspection InfiniteLoopStatement
        while (true) {
          WatchKey key = watcher.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == ENTRY_DELETE) {
              removeMod(modsDirectory.resolve((Path) event.context()));
            }
          }
          key.reset();
        }
      }
    });
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
  public ObservableList<ModInfoBean> getInstalledMods() {
    return readOnlyInstalledMods;
  }

  @Override
  public CompletionStage<Void> downloadAndInstallMod(String uid) {
    return downloadAndInstallMod(fafService.getMod(uid), null, null);
  }

  @Override
  public CompletionStage<Void> downloadAndInstallMod(URL url) {
    return downloadAndInstallMod(url, null, null);
  }

  @Override
  public CompletionStage<Void> downloadAndInstallMod(URL url, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
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
  public CompletionStage<Void> downloadAndInstallMod(ModInfoBean modInfoBean, @Nullable DoubleProperty progressProperty, StringProperty titleProperty) {
    return downloadAndInstallMod(modInfoBean.getDownloadUrl(), progressProperty, titleProperty);
  }

  @Override
  public Set<String> getInstalledModUids() {
    return getInstalledMods().stream()
        .map(ModInfoBean::getId)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getInstalledUiModsUids() {
    return getInstalledMods().stream()
        .filter(ModInfoBean::getUiOnly)
        .map(ModInfoBean::getId)
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
    return getInstalledUiModsUids().contains(uid) || getInstalledModUids().contains(uid);
  }

  @Override
  public CompletionStage<Void> uninstallMod(ModInfoBean mod) {
    UninstallModTask task = applicationContext.getBean(UninstallModTask.class);
    task.setMod(mod);
    return taskService.submitTask(task).getFuture();
  }

  @Override
  public Path getPathForMod(ModInfoBean mod) {
    for (Map.Entry<Path, ModInfoBean> entry : pathToMod.entrySet()) {
      ModInfoBean modInfoBean = entry.getValue();

      if (mod.getId().equals(modInfoBean.getId())) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getAvailableMods() {
    return CompletableFuture.supplyAsync(() -> {
      List<ModInfoBean> availableMods = fafService.getMods();
      ModInfoBeanIterator iterator = new ModInfoBeanIterator(availableMods.iterator());
      noCatch(() -> suggester.build(iterator));
      return availableMods;
    }, threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getMostDownloadedMods(int count) {
    return getTopElements(ModInfoBean.DOWNLOADS_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getMostLikedMods(int count) {
    return getTopElements(ModInfoBean.LIKES_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getMostPlayedMods(int count) {
    return getTopElements(ModInfoBean.TIMES_PLAYED_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getNewestMods(int count) {
    return getTopElements(ModInfoBean.PUBLISH_DATE_COMPARATOR.reversed(), count);
  }

  @Override
  public CompletionStage<List<ModInfoBean>> getMostLikedUiMods(int count) {
    return getAvailableMods().thenApply(modInfoBeans -> modInfoBeans.stream()
        .filter(ModInfoBean::getUiOnly)
        .sorted(ModInfoBean.LIKES_COMPARATOR.reversed())
        .limit(count)
        .collect(Collectors.toList()));
  }

  @Override
  public CompletionStage<List<ModInfoBean>> lookupMod(String string, int maxResults) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        LOOKUP_LOCK.lock();
        ModInfoBeanIterator iterator = new ModInfoBeanIterator(fafService.getMods().iterator());
        suggester.build(iterator);
        return suggester.lookup(string, maxResults, true, false).stream()
            .map(lookupResult -> iterator.deserialize(lookupResult.payload.bytes))
            .collect(Collectors.toList());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        LOOKUP_LOCK.unlock();
      }
    }, threadPoolExecutor).exceptionally(throwable -> {
      logger.warn("Lookup failed", throwable);
      return null;
    });
  }

  @NotNull
  public ModInfoBean extractModInfo(Path path) {
    ModInfoBean modInfoBean = new ModInfoBean();

    Path modInfoLua = path.resolve("mod_info.lua");
    if (Files.notExists(modInfoLua)) {
      throw new ModLoadException("Missing mod_info.lua in: " + path.toAbsolutePath());
    }

    logger.debug("Reading mod {}", path);

    try {
      LuaValue luaValue = noCatch(() -> loadFile(modInfoLua), ModLoadException.class);

      modInfoBean.setId(luaValue.get("uid").toString());
      modInfoBean.setName(luaValue.get("name").toString());
      modInfoBean.setDescription(luaValue.get("description").toString());
      modInfoBean.setAuthor(luaValue.get("author").toString());
      modInfoBean.setVersion(new ComparableVersion(luaValue.get("version").toString()));
      modInfoBean.setSelectable(luaValue.get("selectable").toboolean());
      modInfoBean.setUiOnly(luaValue.get("ui_only").toboolean());
      modInfoBean.setImagePath(extractIconPath(path, luaValue));
    } catch (LuaError e) {
      throw new ModLoadException(e);
    }

    return modInfoBean;
  }

  @Override
  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = applicationContext.getBean(ModUploadTask.class);
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  @Override
  @Cacheable(value = CacheNames.MOD_THUMBNAIL, unless = "#result == null")
  public Image loadThumbnail(ModInfoBean mod) {
    URL url = mod.getThumbnailUrl();
    if (url == null) {
      return IdenticonUtil.createIdenticon(mod.getId());
    }

    logger.debug("Fetching thumbnail for mod {} from {}", mod.getName(), url);
    Image image = assetService.loadAndCacheImage(url, Paths.get("mods"), null);
    return image != null ? image : IdenticonUtil.createIdenticon(mod.getId());
  }

  @Override
  public void evictModsCache() {
    fafService.evictModsCache();
  }

  @Override
  public ComparableVersion readModVersion(Path modDirectory) {
    return extractModInfo(modDirectory).getVersion();
  }

  @Override
  public CompletableFuture<List<FeaturedModBean>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }


  @Override
  public CompletableFuture<FeaturedModBean> getFeaturedMod(String featuredMod) {
    return getFeaturedMods().thenCompose(featuredModBeans -> completedFuture(featuredModBeans.stream()
        .filter(featuredModBean -> featuredMod.equals(featuredModBean.getTechnicalName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Not a valid featured mod: " + featuredMod))
    ));
  }

  @Override
  public List<MountPoint> readMountPoints(InputStream inputStream, Path basePath) {
    LuaValue modInfo = noCatch(() -> load(inputStream));
    ArrayList<MountPoint> mountPoints = new ArrayList<>();
    LuaTable mountpoints = modInfo.get("mountpoints").checktable();
    for (LuaValue key : mountpoints.keys()) {
      mountPoints.add(new MountPoint(basePath.resolve(key.tojstring()), mountpoints.get(key).tojstring()));
    }
    return mountPoints;
  }

  private CompletionStage<List<ModInfoBean>> getTopElements(Comparator<? super ModInfoBean> comparator, int count) {
    return getAvailableMods().thenApply(modInfoBeans -> modInfoBeans.stream()
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

  private void removeMod(Path path) throws IOException {
    installedMods.remove(pathToMod.remove(path));
  }

  private void addMod(Path path) {
    try {
      ModInfoBean modInfoBean = extractModInfo(path);
      pathToMod.put(path, modInfoBean);
      if (!installedMods.contains(modInfoBean)) {
        installedMods.add(modInfoBean);
      }
    } catch (ModLoadException e) {
      logger.debug("Corrupt mod: " + path, e);

      notificationService.addNotification(new PersistentNotification(i18n.get("corruptedMods.notification", path.getFileName()), WARN, singletonList(
          new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(path))
      )));
    }
  }
}
