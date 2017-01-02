package com.faforever.client.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.Bytes;
import com.faforever.client.replay.Replay.ChatMessage;
import com.faforever.client.replay.Replay.GameOption;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayDetailController implements Controller<Node> {

  private final TimeService timeService;
  private final I18n i18n;
  private final UiService uiService;
  private final ReplayService replayService;
  public Node replayDetailRoot;
  public Label titleLabel;
  public Label dateLabel;
  public Label timeLabel;
  public Label modLabel;
  public Label durationLabel;
  public Label playerCountLabel;
  public Label ratingLabel;
  public Label qualityLabel;
  public Label numberOfReviewsLabel;
  public Label mapLabel;
  public HBox teamsContainer;
  public HBox ratingContainer;
  public TableView<ChatMessage> chatTable;
  public TableColumn<ChatMessage, Duration> chatGameTimeColumn;
  public TableColumn<ChatMessage, String> chatSenderColumn;
  public TableColumn<ChatMessage, String> chatMessageColumn;
  public TableView<GameOption> optionsTable;
  public TableColumn<GameOption, String> optionKeyColumn;
  public TableColumn<GameOption, String> optionValueColumn;
  public Button downloadMoreInfoButton;
  public Pane moreInformationPane;
  private Replay replay;

  public ReplayDetailController(TimeService timeService, I18n i18n, UiService uiService, ReplayService replayService) {
    this.timeService = timeService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.replayService = replayService;
  }

  public void initialize() {
    chatGameTimeColumn.setCellValueFactory(param -> param.getValue().timeProperty());
    chatGameTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asHms));

    chatSenderColumn.setCellValueFactory(param -> param.getValue().senderProperty());
    chatSenderColumn.setCellFactory(param -> new StringCell<>(String::toString));

    chatMessageColumn.setCellValueFactory(param -> param.getValue().senderProperty());
    chatMessageColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionKeyColumn.setCellValueFactory(param -> param.getValue().keyProperty());
    optionKeyColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionValueColumn.setCellValueFactory(param -> param.getValue().valueProperty());
    optionValueColumn.setCellFactory(param -> new StringCell<>(String::toString));

    optionsTable.managedProperty().bind(optionsTable.visibleProperty());
    optionsTable.setVisible(false);

    chatTable.managedProperty().bind(chatTable.visibleProperty());
    chatTable.setVisible(false);

    downloadMoreInfoButton.managedProperty().bind(downloadMoreInfoButton.visibleProperty());
    moreInformationPane.managedProperty().bind(moreInformationPane.visibleProperty());
    moreInformationPane.setVisible(false);
  }

  public void setReplay(Replay replay) {
    this.replay = replay;
    titleLabel.setText(replay.getTitle());
    dateLabel.setText(timeService.asDate(replay.getStartTime()));
    timeLabel.setText(timeService.asShortTime(replay.getStartTime()));

    Instant endTime = replay.getEndTime();
    if (endTime != null) {
      durationLabel.setText(timeService.shortDuration(Duration.between(endTime, replay.getStartTime())));
    } else {
      durationLabel.setVisible(false);
    }

    modLabel.setText(replay.getFeaturedMod().getDisplayName());
    playerCountLabel.setText(i18n.number(replay.getTeams().values().stream().mapToInt(List::size).sum()));
    // TODO get human readable map name instead of technical name
    mapLabel.setText(i18n.get("game.onMapFormat", replay.getMap()));

    // FIXME implement
    ratingLabel.setText("n/a");
    qualityLabel.setText("n/a");
    numberOfReviewsLabel.setText("0");

    downloadMoreInfoButton.setText(i18n.get("game.downloadMoreInfo",
        Bytes.formatSize(replayService.getSize(replay.getId()), i18n.getUserSpecificLocale())));

    // These items are initially empty but will be populated in #onDownloadMoreInfoClicked()
    optionsTable.setItems(replay.getGameOptions());
    chatTable.setItems(replay.getChatMessages());

    populateTeamsContainer(replay.getTeams());
    populateRatingContainer();
  }

  public void onDownloadMoreInfoClicked() {
    // TODO display loading indicator
    downloadMoreInfoButton.setVisible(false);
    replayService.downloadReplay(replay.getId()).thenAccept(path -> replayService.enrich(replay, path));
    optionsTable.setVisible(true);
    chatTable.setVisible(true);
    moreInformationPane.setVisible(true);
  }

  private void populateRatingContainer() {
    // FIXME implement
  }

  private void populateTeamsContainer(ObservableMap<String, List<String>> teams) {
    Platform.runLater(() -> teams.entrySet().forEach(entry -> {
      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      controller.setPlayersInTeam(entry.getKey(), entry.getValue());
      teamsContainer.getChildren().add(controller.getRoot());
    }));
  }

  @Override
  public Node getRoot() {
    return replayDetailRoot;
  }

  public void onCloseButtonClicked() {
    ((Pane) replayDetailRoot.getParent()).getChildren().remove(replayDetailRoot);
  }

  public void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }
}
