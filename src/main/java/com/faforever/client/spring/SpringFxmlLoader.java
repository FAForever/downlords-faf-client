package com.faforever.client.spring;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;

public class SpringFxmlLoader {

  private ApplicationContext context;

  public SpringFxmlLoader(ApplicationContext context) {
    this.context = context;
  }

  public Object load(String url, Class<?> controllerClass) throws IOException {
    try (InputStream fxmlStream = controllerClass.getResourceAsStream(url)) {
      Object instance = context.getBean(controllerClass);
      FXMLLoader loader = new FXMLLoader();
      loader.getNamespace().put(FXMLLoader.CONTROLLER_KEYWORD, instance);
      return loader.load(fxmlStream);
    }
  }
}
