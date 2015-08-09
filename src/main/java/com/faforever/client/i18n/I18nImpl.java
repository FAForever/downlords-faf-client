package com.faforever.client.i18n;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Locale;

public class I18nImpl implements I18n {

  @Autowired
  MessageSource messageSource;

  @Autowired
  Locale locale;

  @Override
  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, locale);
  }
}
