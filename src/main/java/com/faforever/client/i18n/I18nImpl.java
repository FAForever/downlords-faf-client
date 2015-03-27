package com.faforever.client.i18n;

import org.springframework.context.MessageSource;

import java.util.Locale;

public class I18nImpl implements I18n {

  MessageSource messageSource;
  Locale locale;

  public I18nImpl(MessageSource messageSource, Locale locale) {
    this.messageSource = messageSource;
    this.locale = locale;
  }

  @Override
  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, locale);
  }
}
