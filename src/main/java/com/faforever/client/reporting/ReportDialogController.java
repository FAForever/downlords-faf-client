package com.faforever.client.reporting;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.WrappingStringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.beans.property.SimpleObjectProperty;
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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
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
  public TableView<ModerationReportBean> reportTable;
  public TableColumn<ModerationReportBean, Integer> idColumn;
  public TableColumn<ModerationReportBean, OffsetDateTime> createTimeColumn;
  public TableColumn<ModerationReportBean, ObservableSet<PlayerBean>> offenderColumn;
  public TableColumn<ModerationReportBean, Integer> gameColumn;
  public TableColumn<ModerationReportBean, String> descriptionColumn;
  public TableColumn<ModerationReportBean, PlayerBean> moderatorColumn;
  public TableColumn<ModerationReportBean, String> noticeColumn;
  public TableColumn<ModerationReportBean, ModerationReportStatus> statusColumn;
  private Window ownerWindow;

  public void initialize() {
    reportTable.setPlaceholder(new Label(i18n.get("report.noReports")));

    idColumn.setCellValueFactory(param -> param.getValue().idProperty());
    idColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    createTimeColumn.setCellValueFactory(param -> param.getValue().createTimeProperty());
    createTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asDateTime));
    offenderColumn.setCellValueFactory(param -> param.getValue().reportedUsersProperty());
    offenderColumn.setCellFactory(param -> new WrappingStringCell<>((players ->
        players.stream().map(PlayerBean::getUsername).collect(Collectors.joining(", ")))));
    gameColumn.setCellValueFactory(param -> Optional.ofNullable(param.getValue().getGame()).map(replay -> replay.idProperty().asObject()).orElse(new SimpleObjectProperty<>()));
    gameColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    descriptionColumn.setCellValueFactory(param -> param.getValue().reportDescriptionProperty());
    descriptionColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    moderatorColumn.setCellValueFactory(param -> param.getValue().lastModeratorProperty());
    moderatorColumn.setCellFactory(param -> new StringCell<>(PlayerBean::getUsername));
    noticeColumn.setCellValueFactory(param -> param.getValue().moderatorNoticeProperty());
    noticeColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    statusColumn.setCellValueFactory(param -> param.getValue().reportStatusProperty());
    statusColumn.setCellFactory(param -> new StringCell<>(status -> i18n.get(status.getI18nKey())));

    updateReportTable();
  }

  public void onReportButtonClicked() {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    ModerationReportBean report = new ModerationReportBean();
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
        Integer.parseInt(gameIdString);
        gameId.setText(gameIdString);
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

  private void submitReport(ModerationReportBean report) {
    playerService.getPlayerByName(offender.getText())
        .thenApply(player -> {
          if (player.isEmpty()) {
            warnNoPlayer();
            return false;
          }

          report.getReportedUsers().add(player.get());
          return true;
        })
        .thenCompose(submit -> {
          if (submit && !gameId.getText().isBlank()) {
            return replayService.findById(Integer.parseInt(gameId.getText())).thenApply(replay -> {
              if (replay.isEmpty()) {
                warnNoGame();
                return false;
              }

              if (replay.get().getTeams().values().stream().flatMap(Collection::stream).noneMatch(username -> username.equals(offender.getText()))) {
                warnOffenderNotInGame();
                return false;
              }

              report.setGame(replay.get());
              return true;
            });
          } else {
            return CompletableFuture.completedFuture(submit);
          }
        }).thenCompose(submit -> {
      if (!submit) {
        return CompletableFuture.completedFuture(null);
      }

      return moderationService.postModerationReport(report);
    }).thenAccept(postedReport -> {
      if (postedReport != null) {
        updateReportTable();
        clearReport();
      }
    }).exceptionally(throwable -> {
      log.warn("Error submitting moderation report", throwable);
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

  public void setOffender(PlayerBean player) {
    offender.setText(player.getUsername());
  }

  public void setOffender(String username) {
    offender.setText(username);
  }

  public void setReplay(ReplayBean replay) {
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
        JavaFxUtil.runLater(() -> reportTable.setItems(FXCollections.observableList(reports))));
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
