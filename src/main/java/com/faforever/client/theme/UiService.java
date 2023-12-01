package com.faforever.client.theme;

import com.faforever.client.config.CacheNames;
import com.faforever.client.exception.FxmlLoadException;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.ui.dialog.Dialog.DialogTransition;
import com.faforever.client.ui.dialog.DialogLayout;
import io.github.sheikah45.fx2j.api.Fx2jLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;


@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class UiService implements InitializingBean {

  private final ThemeService themeService;
  private final MessageSource messageSource;
  private final ApplicationContext applicationContext;
  private final I18n i18n;

  private final ReentrantLock fxmlLoadLock = new ReentrantLock(true);

  private MessageSourceResourceBundle resources;

  @Override
  public void afterPropertiesSet() throws IOException {
    resources = new MessageSourceResourceBundle(messageSource, i18n.getUserSpecificLocale());
  }

  /**
   * Loads an image with caching.
   */
  @Cacheable(value = CacheNames.IMAGES, sync = true)
  public Image getImage(String relativeImage) {
    return new Image(relativeImage, true);
  }

  /**
   * Loads an FXML file and returns its controller instance. The controller instance is retrieved from the application
   * context, so its scope (which should always be "prototype") depends on the bean definition.
   */
  public <T extends Controller<?>> T loadFxml(String relativePath) {
    fxmlLoadLock.lock();
    try {
      Fx2jLoader loader = new Fx2jLoader();
      loader.setLocation(themeService.getThemeFileUrl(relativePath));
      loader.setResources(resources);
      loader.setControllerFactory(applicationContext::getBean);
      loader.load();
      return loader.getController();
    } catch (IOException e) {
      throw new FxmlLoadException("Could not load fxml " + relativePath, e, "fxml.loadError", relativePath);
    } finally {
      fxmlLoadLock.unlock();
    }
  }

  public <T extends Controller<?>> T loadFxml(String relativePath, Class<?> controllerClass) {
    fxmlLoadLock.lock();
    try {
      Fx2jLoader loader = new Fx2jLoader();
      loader.setLocation(themeService.getThemeFileUrl(relativePath));
      loader.setResources(resources);
      loader.setControllerFactory(applicationContext::getBean);
      loader.setController(applicationContext.getBean(controllerClass));
      loader.load();
      return loader.getController();
    } catch (IOException e) {
      throw new FxmlLoadException("Could not load fxml " + relativePath + "with class " + controllerClass.getSimpleName(),
          e, "fxml.loadError", relativePath);
    } finally {
      fxmlLoadLock.unlock();
    }
  }

  public Dialog showInDialog(StackPane parent, Node content) {
    return showInDialog(parent, content, null);
  }

  public Dialog showInDialog(StackPane parent, Node content, String title) {
    DialogLayout dialogLayout = new DialogLayout();
    if (title != null) {
      dialogLayout.setHeading(new Label(title));
    }
    dialogLayout.setBody(content);

    Dialog dialog = new Dialog();
    dialog.setContent(dialogLayout);
    dialog.setTransitionType(DialogTransition.TOP);

    parent.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        dialog.close();
      }
    });

    dialog.show(parent);
    return dialog;
  }

  public void makeScrollableDialog(Dialog dialog) {
    Region dialogContent = dialog.getContent();
    ScrollPane scrollPane = new ScrollPane(dialogContent);
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    JavaFxUtil.bind(scrollPane.prefHeightProperty(), dialogContent.heightProperty());
    dialog.setContent(scrollPane);
  }
}
