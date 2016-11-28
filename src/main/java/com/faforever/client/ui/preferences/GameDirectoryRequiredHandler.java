package com.faforever.client.ui.preferences;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static javafx.application.Platform.runLater;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameDirectoryRequiredHandler {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final EventBus eventBus;
  private final I18n i18n;
  private final Stage stage;
  private final PreferencesService preferencesService;

  public GameDirectoryRequiredHandler(EventBus eventBus, I18n i18n, Stage stage, PreferencesService preferencesService) {
    this.eventBus = eventBus;
    this.i18n = i18n;
    this.stage = stage;
    this.preferencesService = preferencesService;
  }

  @PostConstruct
  public void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onChooseGameDirectory(GameDirectoryChooseEvent event) {
    runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("missingGamePath.chooserTitle"));
      File result = directoryChooser.showDialog(stage.getScene().getWindow());

      logger.info("User selected game directory: {}", result);

      eventBus.post(new GameDirectoryChosenEvent(Optional.ofNullable(result).map(File::toPath).orElse(null)));
    });
  }
}
