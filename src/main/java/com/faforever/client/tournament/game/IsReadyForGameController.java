package com.faforever.client.tournament.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.progress.RingProgressIndicator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IsReadyForGameController implements Controller<Parent> {
  private final I18n i18n;
  public VBox root;
  public Label description;
  public RingProgressIndicator progressIndicator;
  public Button isReadyButton;
  private int timeLeft;
  @VisibleForTesting
  Timeline queuePopTimeUpdater;
  @Setter
  private Runnable readyCallback;
  @Setter
  private Runnable dismissCallBack;
  private boolean clickedReady = false;


  @Override
  public void initialize() {
    progressIndicator.setProgressLabelStringConverter(new StringConverter<>() {
      @Override
      public String toString(Integer object) {
        return i18n.number(timeLeft);
      }

      @Override
      public Integer fromString(String string) {
        throw new UnsupportedOperationException();
      }
    });
  }


  @Override
  public Parent getRoot() {
    return root;
  }

  public void setTimeout(int responseTimeSeconds) {
    OffsetDateTime start = OffsetDateTime.now();

    queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      updateTimer(responseTimeSeconds, start);
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  private void updateTimer(int responseTimeSeconds, OffsetDateTime start) {
    OffsetDateTime now = OffsetDateTime.now();
    Duration timeGone = Duration.between(start, now);
    final var percent = timeGone.toSeconds() / (double) responseTimeSeconds;
    this.timeLeft = (int) (responseTimeSeconds - timeGone.toSeconds());
    progressIndicator.setProgress((int) (percent * 100));
    if (timeLeft <= 0 && queuePopTimeUpdater != null) {
      queuePopTimeUpdater.stop();
      JavaFxUtil.runLater(this::end);
    }
  }

  private void end() {
    if (clickedReady) {
      isReadyButton.setText(i18n.get("isReady.launching"));
    } else {
      dismissCallBack.run();
    }
  }

  public void onReady() {
    readyCallback.run();
    isReadyButton.setDisable(true);
    clickedReady = true;
    isReadyButton.setText(i18n.get("isReady.waiting"));
  }
}
