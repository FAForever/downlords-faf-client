package com.faforever.client.i18n;

import com.faforever.client.preferences.LanguageInfo;
import com.faforever.client.preferences.PreferencesService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Locale;

@Service
public class I18n {

  private final MessageSource messageSource;
  private final PreferencesService preferencesService;

  private Locale userSpecificLocale;

  @Inject
  public I18n(MessageSource messageSource, PreferencesService preferencesService) {
    this.messageSource = messageSource;
    this.preferencesService = preferencesService;
  }

  @PostConstruct
  public void postConstruct() {
    LanguageInfo languageInfo = preferencesService.getPreferences().getLocalization().getLanguage();
    if (!languageInfo.equals(LanguageInfo.AUTO)) {
      userSpecificLocale = new Locale(languageInfo.getLanguageCode(), languageInfo.getCountryCode());
    } else {
      userSpecificLocale = Locale.getDefault();
    }
  }

  
  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, userSpecificLocale);
  }

  
  public Locale getUserSpecificLocale() {
    return this.userSpecificLocale;
  }

  
  public String getQuantized(String singularKey, String pluralKey, long arg) {
    Object[] args = {arg};
    if (Math.abs(arg) == 1) {
      return messageSource.getMessage(singularKey, args, userSpecificLocale);
    }
    return messageSource.getMessage(pluralKey, args, userSpecificLocale);
  }

  
  public String number(int number) {
    return String.format(userSpecificLocale, "%d", number);
  }

  
  public String numberWithSign(int number) {
    return String.format(userSpecificLocale, "%+d", number);
  }

  
  public String number(double number) {
    return String.format(userSpecificLocale, "%f", number);
  }

  
  public String rounded(double number, int digits) {
    return String.format(userSpecificLocale, "%." + digits + "f", number);
  }
}
