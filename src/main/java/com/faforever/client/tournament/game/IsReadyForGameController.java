package com.faforever.client.tournament.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.progress.RingProgressIndicator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.TimerTask;


@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IsReadyForGameController implements Controller<Parent> {
  public static final int MILLIS_TIME_BEFORE_CLOSE = 1000;
  private final I18n i18n;
  public HBox root;
  public Label description;
  public RingProgressIndicator progressIndicator;
  private int timeLeft;
  private Timeline queuePopTimeUpdater;
  @Setter
  private Runnable timedOut;


  @Override
  public void initialize() {
    progressIndicator.setProgressLableStringConverter(new StringConverter<>() {
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
      updateQueue(responseTimeSeconds, start);
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  private void updateQueue(int responseTimeSeconds, OffsetDateTime start) {
    OffsetDateTime now = OffsetDateTime.now();
    Duration timeGone = Duration.between(start, now);
    final var percent = timeGone.toSeconds() / (double) responseTimeSeconds;
    this.timeLeft = (int) (responseTimeSeconds - timeGone.toSeconds());
    progressIndicator.setProgress((int) (percent * 100));
    if(timeLeft <= 0 && queuePopTimeUpdater != null){
      end();
    }
  }

  private void end() {
    queuePopTimeUpdater.stop();
    try {
      Thread.sleep(MILLIS_TIME_BEFORE_CLOSE);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    timedOut.run();
  }
}
