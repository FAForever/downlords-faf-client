package com.faforever.client.ui.preferences;

import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static javafx.application.Platform.runLater;


@Component
@RequiredArgsConstructor
public class GameDirectoryRequiredHandler implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final EventBus eventBus;
  private final I18n i18n;
  private CompletableFuture<Path> future;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onChooseGameDirectory(GameDirectoryChooseEvent event) {
    runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("missingGamePath.chooserTitle"));
      File result = directoryChooser.showDialog(StageHolder.getStage().getScene().getWindow());

      logger.info("User selected game directory: {}", result);

      Path path = Optional.ofNullable(result).map(File::toPath).orElse(null);
      eventBus.post(new GameDirectoryChosenEvent(path, event.getFuture()));

    });
  }

}
