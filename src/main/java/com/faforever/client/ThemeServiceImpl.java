package com.faforever.client;

import com.faforever.client.preferences.PreferencesService;

import javax.annotation.Resource;

public class ThemeServiceImpl implements ThemeService {

  @Resource
  PreferencesService preferencesService;

  @Override
  public String getThemeFile(String relativeFile) {
    return String.format("/themes/%s/%s", preferencesService.getPreferences().getTheme(), relativeFile);
  }
}
