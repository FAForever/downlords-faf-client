package com.faforever.client.fxml;

import javafx.fxml.FXMLLoader;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class FxmlLoaderImpl implements FxmlLoader {

  private MessageSource messageSource;
  private Locale locale;
  private String theme;

  public FxmlLoaderImpl(MessageSource messageSource, Locale locale) {
    this.messageSource = messageSource;
    this.locale = locale;
  }


  @Override
  public void setTheme(String theme) {
    this.theme = theme;
  }

  @Override
  public <T> T loadAndGetController(String file) {
    return load(file, null, null).getController();
  }

  @Override
  public void loadCustomControl(String file, Object control) {
    load(file, control, control);
  }

  private URL buildResourceUrl(String file) throws IOException {
    String path = String.format("/themes/%s/%s", theme, file);
    return new ClassPathResource(path).getURL();
  }

  private FXMLLoader load(String file, Object controller, Object root) {
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setController(controller);
      loader.setRoot(root);
      loader.setLocation(buildResourceUrl(file));
      loader.setResources(new MessageSourceResourceBundle(messageSource, locale));
      loader.load();
      return loader;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
