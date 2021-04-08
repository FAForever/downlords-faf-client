package com.faforever.client.ui.preferences;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class GameDirectoryRequiredHandler implements InitializingBean {

  private final EventBus eventBus;
  private final I18n i18n;
  private CompletableFuture<Path> future;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onChooseGameDirectory(GameDirectoryChooseEvent event) {
    JavaFxUtil.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("missingGamePath.chooserTitle"));
      File result = directoryChooser.showDialog(StageHolder.getStage().getScene().getWindow());

      log.info("User selected game directory: {}", result);

      Path path = Optional.ofNullable(result).map(File::toPath).orElse(null);
      eventBus.post(new GameDirectoryChosenEvent(path, event.getFuture()));

    });
  }

}
