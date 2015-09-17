package com.faforever.client.mod;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModServiceImpl implements ModService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern MOD_ICON_FILE_PATTERN = Pattern.compile("/(\\w+\\.\\w{3,4})\"?$");
  private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("\"(.+)\"");

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  TaskService taskService;

  @Autowired
  ApplicationContext applicationContext;

  private Path modsDirectory;

  @PostConstruct
  void postConstruct() {
    modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
  }

  @Override
  public void addOnGameTypeListener(OnGameTypeInfoListener onGameTypeInfoListener) {
    lobbyServerAccessor.addOnGameTypeInfoListener(onGameTypeInfoListener);
  }

  @Override
  public List<ModInfoBean> getInstalledMods() throws IOException {
    List<ModInfoBean> mods = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory)) {
      for (Path path : directoryStream) {
        mods.add(extractModInfo(path));
      }
    }
    return mods;
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMod(String modPath) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    DownloadModTask task = applicationContext.getBean(DownloadModTask.class);
    task.setModPath(modPath);


    taskService.submitTask(TaskGroup.NET_HEAVY, task, new Callback<Void>() {
      @Override
      public void success(Void result) {
        future.complete(result);
      }

      @Override
      public void error(Throwable e) {
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  @Override
  public Set<String> getInstalledModUids() throws IOException {
    return getInstalledMods().stream()
        .map(ModInfoBean::getUid)
        .collect(Collectors.toSet());
  }

  private ModInfoBean extractModInfo(Path path) throws IOException {
    ModInfoBean modInfoBean = new ModInfoBean();

    try (InputStream inputStream = Files.newInputStream(path.resolve("mod_info.lua"))) {
      Properties properties = new Properties();
      properties.load(inputStream);

      modInfoBean.setUid(stripQuotes(properties.getProperty("uid")));
      modInfoBean.setName(stripQuotes(properties.getProperty("name")));
      modInfoBean.setDescription(stripQuotes(properties.getProperty("description")));
      modInfoBean.setAuthor(stripQuotes(properties.getProperty("author")));
      modInfoBean.setVersion(stripQuotes(properties.getProperty("version")));
      modInfoBean.setSelectable(Boolean.parseBoolean(stripQuotes(properties.getProperty("selectable"))));
      modInfoBean.setUiOnly(Boolean.parseBoolean(stripQuotes(properties.getProperty("ui_only"))));
      modInfoBean.setImagePath(extractIconPath(path, properties));
    }

    return modInfoBean;
  }

  private static String stripQuotes(String string) {
    if (string == null) {
      return null;
    }

    Matcher matcher = QUOTED_TEXT_PATTERN.matcher(string);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return string;
  }

  private static Path extractIconPath(Path path, Properties properties) {
    String icon = properties.getProperty("icon");
    if (icon == null) {
      return null;
    }

    Matcher matcher = MOD_ICON_FILE_PATTERN.matcher(icon);
    if (!matcher.matches()) {
      return null;
    }
    return path.resolve(matcher.group(1));
  }
}
