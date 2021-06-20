package com.faforever.client.reporting;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.WrappingStringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ReportDialogController implements Controller<Node> {

  private final ModerationService moderationService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TimeService timeService;
  private final ReplayService replayService;

  public VBox reportDialogRoot;
  public Label reportLabel;
  public TextField offender;
  public TextArea reportDescription;
  public TextField gameId;
  public TextField gameTime;
  public TableView<ModerationReport> reportTable;
  public TableColumn<ModerationReport, Number> idColumn;
  public TableColumn<ModerationReport, LocalDateTime> createTimeColumn;
  public TableColumn<ModerationReport, ObservableSet<Player>> offenderColumn;
  public TableColumn<ModerationReport, Integer> gameColumn;
  public TableColumn<ModerationReport, String> descriptionColumn;
  public TableColumn<ModerationReport, Player> moderatorColumn;
  public TableColumn<ModerationReport, String> noticeColumn;
  public TableColumn<ModerationReport, ModerationReportStatus> statusColumn;
  private Window ownerWindow;

  public void initialize() {
    reportTable.setPlaceholder(new Label(i18n.get("report.noReports")));

    idColumn.setCellValueFactory(param -> param.getValue().reportIdProperty());
    idColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    createTimeColumn.setCellValueFactory(param -> param.getValue().createTimeProperty());
    createTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asDateTime));
    offenderColumn.setCellValueFactory(param -> param.getValue().reportedUsersProperty());
    offenderColumn.setCellFactory(param -> new WrappingStringCell<>((players ->
        players.stream().map(Player::getUsername).collect(Collectors.joining(", ")))));
    gameColumn.setCellValueFactory(param -> param.getValue().gameIdProperty());
    gameColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    descriptionColumn.setCellValueFactory(param -> param.getValue().reportDescriptionProperty());
    descriptionColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    moderatorColumn.setCellValueFactory(param -> param.getValue().lastModeratorProperty());
    moderatorColumn.setCellFactory(param -> new StringCell<>(Player::getUsername));
    noticeColumn.setCellValueFactory(param -> param.getValue().moderatorNoticeProperty());
    noticeColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    statusColumn.setCellValueFactory(param -> param.getValue().reportStatusProperty());
    statusColumn.setCellFactory(param -> new StringCell<>(status -> i18n.get(status.getI18nKey())));

    updateReportTable();
  }

  public void onReportButtonClicked() {
    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player must be set"));
    ModerationReport report = new ModerationReport();
    report.setReporter(currentPlayer);
    report.setReportDescription(reportDescription.getText());
    report.setGameIncidentTimeCode(gameTime.getText());

    if (offender.getText().isBlank()) {
      warnNoOffender();
      return;
    }

    if (reportDescription.getText().isBlank()) {
      warnNoDescription();
      return;
    }

    String gameIdString = gameId.getText();
    if (!gameIdString.isBlank()) {
      gameIdString = gameIdString.replace("#", "");
      try {
        report.setGameId(Integer.parseInt(gameIdString));
      } catch (NumberFormatException e) {
        warnNonNumericGameId();
        return;
      }
      if (gameTime.getText().isBlank()) {
        warnNoGameTime();
        return;
      }
    }

    submitReport(report);
  }

  private void submitReport(ModerationReport report) {
    playerService.getPlayerByName(offender.getText())
        .thenApply(player -> {
          if (player.isEmpty()) {
            warnNoPlayer();
            return false;
          } else {
            report.getReportedUsers().add(player.get());
            return true;
          }
        })
        .thenCompose(submit -> {
          if (submit && report.getGameId() != null) {
            return replayService.findById(report.getGameId()).thenApply(replay -> {
              if (replay.isPresent()) {
                if (replay.get().getTeams().values().stream().flatMap(Collection::stream).anyMatch(username -> username.equals(offender.getText()))) {
                  return true;
                } else {
                  warnOffenderNotInGame();
                  return false;
                }
              } else {
                warnNoGame();
                return false;
              }
            });
          } else {
            return CompletableFuture.completedFuture(submit);
          }
        }).thenAccept(submit -> {
      if (submit) {
        moderationService.postModerationReport(report);
        updateReportTable();
        clearReport();
      }
    }).exceptionally(throwable -> {
      notificationService.addImmediateErrorNotification(throwable, "report.error");
      return null;
    });
  }

  private void warnNoOffender() {
    notificationService.addImmediateWarnNotification("report.warning.noOffender");
  }

  private void warnOffenderNotInGame() {
    notificationService.addImmediateWarnNotification("report.warning.noOffenderInGame");
  }

  private void warnNoDescription() {
    notificationService.addImmediateWarnNotification("report.warning.noDescription");
  }

  private void warnNoPlayer() {
    log.info(String.format("No player named %s", offender.getText()));
    notificationService.addImmediateWarnNotification("report.warning.noPlayer");
  }

  private void warnNoGameTime() {
    notificationService.addImmediateWarnNotification("report.warning.noGameTime");
  }

  private void warnNonNumericGameId() {
    log.info(String.format("GameId %s not numeric", gameId.getText()));
    notificationService.addImmediateWarnNotification("report.warning.gameIdNotNumeric");
  }

  private void warnNoGame() {
    log.info(String.format("Game %s does not exist", gameId.getText()));
    notificationService.addImmediateWarnNotification("report.warning.title");
  }

  public void setOffender(Player player) {
    offender.setText(player.getUsername());
  }

  public void setOffender(String username) {
    offender.setText(username);
  }

  public void setReplay(Replay replay) {
    TextFields.bindAutoCompletion(offender, replay.getTeams().values().stream().flatMap(Collection::stream)
        .collect(Collectors.toList()));
    gameId.setText(String.valueOf(replay.getId()));
  }

  public void setAutoCompleteWithOnlinePlayers() {
    TextFields.bindAutoCompletion(offender, playerService.getPlayerNames());
  }

  private void clearReport() {
    offender.setText("");
    reportDescription.setText("");
    gameId.setText("");
    gameTime.setText("");
  }

  private void updateReportTable() {
    moderationService.getModerationReports().thenAccept(reports ->
        JavaFxUtil.runLater(() -> reportTable.setItems(FXCollections.observableList(reports.stream()
            .filter(report -> report.getCreateTime().isAfter(LocalDateTime.now().minusYears(1)))
            .sorted(Comparator.comparing(ModerationReport::getCreateTime).reversed())
            .collect(Collectors.toList())))));
  }

  public Pane getRoot() {
    return reportDialogRoot;
  }

  public void show() {
    Assert.checkNullIllegalState(ownerWindow, "ownerWindow must be set");

    FxStage fxStage = FxStage.create(reportDialogRoot)
        .initOwner(ownerWindow)
        .initModality(Modality.WINDOW_MODAL)
        .withSceneFactory(uiService::createScene)
        .allowMinimize(false)
        .apply();

    Stage stage = fxStage.getStage();
    stage.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        reportDialogRoot.getChildren().clear();
      }
    });
    stage.show();
  }

  public void setOwnerWindow(Window ownerWindow) {
    this.ownerWindow = ownerWindow;
  }

}
