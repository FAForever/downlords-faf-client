package com.faforever.client.i18n;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.springframework.context.MessageSource;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Locale;

public class I18nImpl implements I18n {

  @Resource
  MessageSource messageSource;
  @Resource
  PreferencesService preferencesService;

  @Resource
  Locale locale;
  @Resource
  private HashMap<String,String> countryCodes ;
  private Locale userSpecificLocal;
  private Preferences preferences;

  @Override
  public String get(String key, Object... args) {
    String languagecode = preferencesService.getPreferences().getLanguagePrefs().getLanguage();
    if(languagecode==null)languagecode=locale.getLanguage();
    return messageSource.getMessage(key, args, new Locale(languagecode,countryCodes.get(languagecode)));
  }

  @Override
  public Locale getLocale() {
    return locale;
  }
}
