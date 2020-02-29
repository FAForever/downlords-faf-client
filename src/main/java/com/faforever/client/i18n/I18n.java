package com.faforever.client.i18n;

import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.Strings;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class I18n implements InitializingBean {
  private static final Pattern MESSAGES_FILE_PATTERN = Pattern.compile("(.*[/\\\\]messages)(?:_([a-z]{2}))(?:_([a-z]{2}))?\\.properties", Pattern.CASE_INSENSITIVE);
  private final ReloadableResourceBundleMessageSource messageSource;
  private final PreferencesService preferencesService;
  private final ObservableSet<Locale> availableLanguages;

  private Locale userSpecificLocale;

  public I18n(ReloadableResourceBundleMessageSource messageSource, PreferencesService preferencesService) {
    this.messageSource = messageSource;
    this.preferencesService = preferencesService;
    availableLanguages = FXCollections.observableSet(new HashSet<>());
  }

  @Override
  public void afterPropertiesSet() throws IOException {
    Locale locale = preferencesService.getPreferences().getLocalization().getLanguage();
    if (locale != null) {
      userSpecificLocale = new Locale(locale.getLanguage(), locale.getCountry());
    } else {
      userSpecificLocale = Locale.getDefault();
    }

    loadAvailableLanguages();
  }

  private void loadAvailableLanguages() throws IOException {
    // These are the default languages shipped with the client
    availableLanguages.addAll(Set.of(
        Locale.US,
        Locale.GERMAN,
        Locale.FRENCH,
        new Locale("ru"),
        Locale.CHINESE,
        new Locale("cs"),
        new Locale("es"),
        new Locale("uk"),
        new Locale("iw"),
        new Locale("tr"),
        new Locale("nl"),
        new Locale("pl")
    ));

    Path languagesDirectory = preferencesService.getLanguagesDirectory();
    if (Files.notExists(languagesDirectory)) {
      return;
    }

    Set<String> currentBaseNames = messageSource.getBasenameSet();
    Set<String> newBaseNames = new LinkedHashSet<>();
    try (Stream<Path> dir = Files.list(languagesDirectory)) {
      dir
          .map(path -> MESSAGES_FILE_PATTERN.matcher(path.toString()))
          .filter(Matcher::matches)
          .forEach(matcher -> {
            newBaseNames.add(Paths.get(matcher.group(1)).toUri().toString());
            availableLanguages.add(new Locale(matcher.group(2), Strings.nullToEmpty(matcher.group(3))));
          });
    }
    // Make sure that current base names are added last; the files above have precedence
    newBaseNames.addAll(currentBaseNames);
    messageSource.setBasenames(newBaseNames.toArray(new String[0]));
  }

  public String get(String key, Object... args) {
    return get(userSpecificLocale, key, args);
  }

  public String get(Locale locale, String key, Object... args) {
    return messageSource.getMessage(key, args, locale);
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

  public String rounded(double number, int digits) {
    return String.format(userSpecificLocale, "%." + digits + "f", number);
  }

  public ReadOnlySetWrapper<Locale> getAvailableLanguages() {
    return new ReadOnlySetWrapper<>(availableLanguages);
  }
}
