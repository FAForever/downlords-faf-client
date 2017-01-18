package com.faforever.client.i18n;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Locale;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class I18nImpl implements I18n {

  private final MessageSource messageSource;

  private final Locale locale;

  @Inject
  public I18nImpl(MessageSource messageSource, Locale locale) {
    this.messageSource = messageSource;
    this.locale = locale;
  }

  @Override
  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, locale);
  }

  @Override
  public String getQuantized(String singularKey, String pluralKey, long arg) {
    Object[] args = {arg};
    if (Math.abs(arg) == 1) {
      return messageSource.getMessage(singularKey, args, locale);
    }
    return messageSource.getMessage(pluralKey, args, locale);
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public String number(int number) {
    return String.format(locale, "%d", number);
  }
}
