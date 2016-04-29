package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import com.github.nocatch.NoCatch;
import javafx.fxml.FXMLLoader;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Locale;
import java.util.concurrent.Callable;

public class FxmlLoaderImpl implements FxmlLoader {

  @Resource
  MessageSource messageSource;
  @Resource
  Locale locale;
  @Resource
  ThemeService themeService;

  private MessageSourceResourceBundle resources;

  @PostConstruct
  void postConstruct() {
    resources = new MessageSourceResourceBundle(messageSource, locale);
  }

  @Override
  public <T> T loadAndGetController(String file) {
    return load(file, null, null).getController();
  }

  @Override
  public void loadCustomControl(String file, Object control) {
    load(file, control, control);
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
    FXMLLoader loader = new FXMLLoader(themeService.getThemeFileUrl(file), resources);
    loader.setController(controller);
    loader.setRoot(root);
    NoCatch.noCatch((Callable<Object>) loader::load);
    return loader;
  }
}
