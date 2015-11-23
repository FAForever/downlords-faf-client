package com.faforever.client.i18n;

import org.springframework.context.MessageSource;

import javax.annotation.Resource;
import java.util.Locale;

public class I18nImpl implements I18n {

  @Resource
  MessageSource messageSource;

  @Resource
  Locale locale;

  @Override
  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, locale);
  }
}
