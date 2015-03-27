package com.faforever.client.fxml;

import javafx.fxml.FXMLLoader;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Locale;

public class FxmlLoaderImpl implements FxmlLoader {

  private MessageSource messageSource;
  private Locale locale;

  public FxmlLoaderImpl(MessageSource messageSource, Locale locale) {
    this.messageSource = messageSource;
    this.locale = locale;
  }

  @Override
  public <T> T load(Resource resource) {
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource.getURL());
      loader.setResources(new MessageSourceResourceBundle(messageSource, locale));
      loader.load();
      return loader.getController();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T load(String file) {
    return load(new ClassPathResource(file));
  }
}
