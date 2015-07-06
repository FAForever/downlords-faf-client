package com.faforever.client.mod;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModServiceImpl implements ModService {

  private static final Pattern MOD_ICON_FILE_PATTERN = Pattern.compile("/(\\w+\\.\\w{3,4})\"?$");

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  PreferencesService preferencesService;

  private Path modsDirectory;

  @PostConstruct
  void postConstruct() {
    modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
  }

  @Override
  public void addOnModInfoListener(OnGameTypeInfoListener onGameTypeInfoListener) {
    lobbyServerAccessor.addOnGameTypeInfoListener(onGameTypeInfoListener);
  }

  @Override
  public void getInstalledModsInBackground(Callback<List<ModInfoBean>> callback) {

    List<ModInfoBean> mods = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory)) {
      for (Path path : directoryStream) {
        mods.add(createModInfoBean(path));
      }
    } catch (IOException e) {
      callback.error(e);
    }

    callback.success(mods);
  }

  private ModInfoBean createModInfoBean(Path path) throws IOException {
    ModInfoBean modInfoBean = new ModInfoBean();
    modInfoBean.setImagePath(extractModIcon(path));
    modInfoBean.setName(path.getFileName().toString());
    return modInfoBean;
  }

  private Path extractModIcon(Path path) throws IOException {
    try (InputStream inputStream = Files.newInputStream(path.resolve("mod_info.lua"))) {
      Properties properties = new Properties();
      properties.load(inputStream);

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
}
