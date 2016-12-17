package com.faforever.client.i18n;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.springframework.context.MessageSource;

import javax.annotation.PostConstruct;
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

  @PostConstruct
  public void setLocale()
  {
    String languagecode = preferencesService.getPreferences().getLanguagePrefs().getLanguage();
    if(languagecode==null)languagecode=locale.getLanguage();
    userSpecificLocal=new Locale(languagecode,countryCodes.get(languagecode));
  }
  @Override
  public String get(String key, Object... args) {

    return messageSource.getMessage(key, args, userSpecificLocal);
  }

  @Override
  public Locale getLocale() {
    return locale;
  }
}
