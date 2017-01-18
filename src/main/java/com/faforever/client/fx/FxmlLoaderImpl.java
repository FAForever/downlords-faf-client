package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Locale;

@Component
@Lazy
public class FxmlLoaderImpl implements FxmlLoader {

  private final MessageSource messageSource;
  private final Locale locale;
  private final UiService uiService;
  private final ApplicationContext applicationContext;

  private MessageSourceResourceBundle resources;

  @Inject
  public FxmlLoaderImpl(MessageSource messageSource, Locale locale, UiService uiService, ApplicationContext applicationContext) {
    this.messageSource = messageSource;
    this.locale = locale;
    this.uiService = uiService;
    this.applicationContext = applicationContext;
  }

  @PostConstruct
  void postConstruct() {
    resources = new MessageSourceResourceBundle(messageSource, locale);
  }

  @Override
  public <T> T loadAndGetController(String file) {
    return load(file, null, null).getController();
  }

  @Override
  public <T> T loadAndGetRoot(String file) {
    return loadAndGetRoot(file, null);
  }

  @Override
  public <T> T loadAndGetRoot(String file, Object controller) {
    return load(file, controller, null).getRoot();
  }

  private FXMLLoader load(String file, Object controller, Object root) {
    try {
      FXMLLoader loader = new FXMLLoader();
//      loader.setControllerFactory(param -> {
//          return applicationContext.getBean(param);
//      });
      loader.setController(controller);
      loader.setRoot(root);
      loader.setLocation(uiService.getThemeFileUrl(file));
      loader.setResources(resources);
      loader.load();
      return loader;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
