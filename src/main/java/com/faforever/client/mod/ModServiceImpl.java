package com.faforever.client.mod;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FileUtils;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.client.util.LuaUtil.loadFile;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Collections.singletonList;

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
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory)) {
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

    return taskService.submitTask(task)
        .thenAccept(aVoid -> loadInstalledMods());
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMod(ModInfoBean modInfoBean, @Nullable DoubleProperty progressProperty, StringProperty titleProperty) {
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
  public CompletableFuture<Void> uninstallMod(ModInfoBean mod) {
    UninstallModTask task = applicationContext.getBean(UninstallModTask.class);
    task.setMod(mod);
    return taskService.submitTask(task);
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
  public CompletableFuture<List<ModInfoBean>> getAvailableMods() {
    return CompletableFuture.supplyAsync(() -> {
          List<ModInfoBean> availableMods = fafService.getMods();

          try {
            ModInfoBeanIterator iterator = new ModInfoBeanIterator(availableMods.iterator());
            suggester.build(iterator);
            return availableMods;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        , threadPoolExecutor);
  }

  @Override
  public CompletableFuture<List<ModInfoBean>> getMostDownloadedMods(int count) {
    return getTopElements(ModInfoBean.DOWNLOADS_COMPARATOR, count);
  }

  @Override
  public CompletableFuture<List<ModInfoBean>> getMostLikedMods(int count) {
    return getTopElements(ModInfoBean.LIKES_COMPARATOR, count);
  }

  @Override
  public CompletableFuture<List<ModInfoBean>> getNewestMods(int count) {
    return getTopElements(ModInfoBean.PUBLISH_DATE_COMPARATOR, count);
  }

  @Override
  public CompletableFuture<List<ModInfoBean>> getMostLikedUiMods(int count) {
    return getAvailableMods().thenApply(modInfoBeans -> modInfoBeans.stream()
        .filter(ModInfoBean::getUiOnly)
        .sorted(ModInfoBean.LIKES_COMPARATOR.reversed())
        .limit(count)
        .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<ModInfoBean>> lookupMod(String string, int maxResults) {
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
      throw new ModLoadFailException("Missing mod_info.lua in: " + path.toAbsolutePath());
    }

    logger.debug("Reading mod {}", path);

    LuaValue luaValue = noCatch(() -> loadFile(modInfoLua), ModLoadFailException.class);

    modInfoBean.setId(luaValue.get("uid").toString());
    modInfoBean.setName(luaValue.get("name").toString());
    modInfoBean.setDescription(luaValue.get("description").toString());
    modInfoBean.setAuthor(luaValue.get("author").toString());
    modInfoBean.setVersion(luaValue.get("version").toString());
    modInfoBean.setSelectable(luaValue.get("selectable").toboolean());
    modInfoBean.setUiOnly(luaValue.get("ui_only").toboolean());
    modInfoBean.setImagePath(extractIconPath(path, luaValue));

    return modInfoBean;
  }

  @Override
  public UploadModTask uploadMod(Path modPath, Consumer<Float> progressListener) {
    UploadModTask uploadModTask = applicationContext.getBean(UploadModTask.class);
    uploadModTask.setModPath(modPath);
    uploadModTask.setProgressListener(progressListener);

    CompletableFuture<Void> uploadFuture = taskService.submitTask(uploadModTask);
    uploadModTask.setFuture(uploadFuture);

    return uploadModTask;
  }

  private CompletableFuture<List<ModInfoBean>> getTopElements(Comparator<? super ModInfoBean> comparator, int count) {
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
    } catch (ModLoadFailException e) {
      logger.debug("Corrupt mod: " + path, e);
      moveCorruptMod(path);
    }
  }

  private void moveCorruptMod(Path path) {
    noCatch(() -> {
      Path corruptedModsDirectory = preferencesService.getCorruptedModsDirectory();
      createDirectories(corruptedModsDirectory);

      logger.debug("Moving corrupted mod file from {} into {}", path, corruptedModsDirectory);

      FileUtils.moveDirectoryInto(path, corruptedModsDirectory);

      notificationService.addNotification(new PersistentNotification(i18n.get("corruptedMods.notification"), WARN, singletonList(
          new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(corruptedModsDirectory.resolve(path.getFileName())))
      )));
    });
  }
}
