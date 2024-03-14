package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingQueueItemController extends NodeController<VBox> {

  private final static String QUEUE_I18N_PATTERN = "teammatchmaking.queue.%s";

  private final UiService uiService;
  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final I18n i18n;
  private final NavigationHandler navigationHandler;

  public VBox queueItemRoot;
  public Label playersInQueueLabel;
  public Label activeGamesLabel;
  public Label queuePopTimeLabel;
  public ToggleButton selectButton;
  public Label searchingLabel;
  public Label matchFoundLabel;
  public Label matchStartingLabel;
  public Label matchCancelledLabel;
  public Button mapPoolButton;

  private final ObjectProperty<MatchmakerQueueInfo> queue = new SimpleObjectProperty<>();
  private final ObservableValue<OffsetDateTime> popTime = queue.flatMap(MatchmakerQueueInfo::queuePopTimeProperty);
  private Timeline queuePopTimeUpdater;
  private Consumer<MatchmakerQueueInfo> onMapPoolClickedListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(matchFoundLabel, matchStartingLabel, matchCancelledLabel);

    queue.when(showing).subscribe(((oldValue, newValue) -> {
      if (oldValue != null) {
        selectButton.selectedProperty().unbindBidirectional(oldValue.selectedProperty());
      }

      if (newValue != null) {
        selectButton.selectedProperty().bindBidirectional(newValue.selectedProperty());
      }
    }));

    selectButton.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);
    mapPoolButton.setText(i18n.get("teammatchmaking.mapPool").toUpperCase());

    ObservableValue<MatchingStatus> matchingStatus = queue.flatMap(MatchmakerQueueInfo::matchingStatusProperty);
    searchingLabel.visibleProperty()
                  .bind(matchingStatus.map(status -> status == MatchingStatus.SEARCHING).orElse(false).when(showing));
    matchFoundLabel.visibleProperty()
                   .bind(
                       matchingStatus.map(status -> status == MatchingStatus.MATCH_FOUND).orElse(false).when(showing));
    matchStartingLabel.visibleProperty()
                      .bind(matchingStatus.map(status -> status == MatchingStatus.GAME_LAUNCHING)
                                          .orElse(false)
                                          .when(showing));
    matchCancelledLabel.visibleProperty()
                       .bind(matchingStatus.map(status -> status == MatchingStatus.MATCH_CANCELLED)
                                           .orElse(false)
                                           .when(showing));

    selectButton.textProperty().bind(queue.flatMap(MatchmakerQueueInfo::technicalNameProperty)
                           .map(technicalName -> i18n.getOrDefault(technicalName,
                                                                   QUEUE_I18N_PATTERN.formatted(technicalName)))
                           .when(showing));

    playersInQueueLabel.textProperty().bind(queue.flatMap(MatchmakerQueueInfo::playersInQueueProperty)
                                  .map(numPlayers -> i18n.get("teammatchmaking.playersInQueue", numPlayers))
                                  .map(String::toUpperCase)
                                  .when(showing));

    activeGamesLabel.textProperty().bind(queue.flatMap(MatchmakerQueueInfo::activeGamesProperty)
                               .map(numPlayers -> i18n.get("teammatchmaking.activeGames", numPlayers))
                               .map(String::toUpperCase)
                               .when(showing));

    BooleanBinding partyTooBig = Bindings.size(teamMatchmakingService.getParty().getMembers())
                                         .greaterThan(IntegerBinding.integerExpression(
                                             queue.flatMap(MatchmakerQueueInfo::teamSizeProperty)));

    BooleanExpression notPartyOwner = BooleanBinding.booleanExpression(teamMatchmakingService.getParty()
                                                                                             .ownerProperty()
                                                                                             .isNotEqualTo(
                                                                                                 playerService.currentPlayerProperty()));

    selectButton.disableProperty()
                .bind(playerService.currentPlayerProperty().isNull()
                                   .or(queue.isNull())
                                   .or(partyTooBig)
                                   .or(teamMatchmakingService.partyMembersNotReadyProperty())
                                   .or(notPartyOwner)
                                   .when(showing));

    setQueuePopTimeUpdater();
  }

  @Override
  public void onShow() {
    if (queuePopTimeUpdater == null) {
      return;
    }
    queuePopTimeUpdater.play();
  }

  @Override
  public void onHide() {
    if (queuePopTimeUpdater == null) {
      return;
    }
    queuePopTimeUpdater.stop();
  }

  @Override
  public VBox getRoot() {
    return queueItemRoot;
  }

  public void setQueue(MatchmakerQueueInfo queue) {
    this.queue.set(queue);
  }

  private void setQueuePopTimeUpdater() {
    queuePopTimeUpdater = new Timeline(new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      OffsetDateTime popTimeValue = popTime.getValue();
      if (popTimeValue != null) {
        OffsetDateTime now = OffsetDateTime.now();
        Duration timeUntilPopQueue = Duration.between(now, popTimeValue);
        if (!timeUntilPopQueue.isNegative()) {
          queuePopTimeLabel.setText(i18n.get("teammatchmaking.queuePopTimer", timeUntilPopQueue.toMinutes(),
                                             timeUntilPopQueue.toSecondsPart())
                                        .toUpperCase());
        }
      }
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  public void setOnMapPoolClickedListener(Consumer<MatchmakerQueueInfo> onMapPoolClickedListener) {
    this.onMapPoolClickedListener = onMapPoolClickedListener;
  }
  public void showMapPool() {
    onMapPoolClickedListener.accept(this.queue.get());
  }

  public MatchmakerQueueInfo getQueue() {
    return queue.get();
  }

  public ObjectProperty<MatchmakerQueueInfo> queueProperty() {
    return queue;
  }
}
