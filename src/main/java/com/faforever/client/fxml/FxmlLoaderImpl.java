package com.faforever.client.fxml;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
  public <T> T loadAndGetController(Resource resource) {
    return load(resource).getController();
  }

  @Override
  public <T> T loadAndGetController(String file) {
    return load(new ClassPathResource(file)).getController();
  }

  @Override
  public <T extends Node> T loadAndGetRoot(String file) {
    return load(new ClassPathResource(file)).getRoot();
  }

  @Override
  public <T extends Node> T loadAndGetRoot(Resource resource) {
    return load(resource).getRoot();
  }

  private FXMLLoader load(Resource resource) {
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource.getURL());
      loader.setResources(new MessageSourceResourceBundle(messageSource, locale));
      loader.load();
      return loader;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
