package com.faforever.client.reporting;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.ObservableConstant;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.WrappingStringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ReportDialogController extends NodeController<Node> {

  private final ModerationService moderationService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final ThemeService themeService;
  private final TimeService timeService;
  private final ReplayService replayService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public VBox reportDialogRoot;
  public Label reportLabel;
  public Button reportButton;
  public TextField offender;
  public TextArea reportDescription;
  public TextField gameId;
  public TextField gameTime;
  public TableView<ModerationReportBean> reportTable;
  public TableColumn<ModerationReportBean, Integer> idColumn;
  public TableColumn<ModerationReportBean, OffsetDateTime> createTimeColumn;
  public TableColumn<ModerationReportBean, Set<PlayerBean>> offenderColumn;
  public TableColumn<ModerationReportBean, Integer> gameColumn;
  public TableColumn<ModerationReportBean, String> descriptionColumn;
  public TableColumn<ModerationReportBean, PlayerBean> moderatorColumn;
  public TableColumn<ModerationReportBean, String> noticeColumn;
  public TableColumn<ModerationReportBean, ModerationReportStatus> statusColumn;
  private Window ownerWindow;

  @Override
  protected void onInitialize() {
    reportTable.setPlaceholder(new Label(i18n.get("report.noReports")));

    idColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().id()));
    idColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    createTimeColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().createTime()));
    createTimeColumn.setCellFactory(param -> new StringCell<>(timeService::asDateTime));
    offenderColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().reportedUsers()));
    offenderColumn.setCellFactory(param -> new WrappingStringCell<>((players ->
        players.stream().map(PlayerBean::getUsername).collect(Collectors.joining(", ")))));
    gameColumn.setCellValueFactory(param -> Optional.ofNullable(param.getValue().game())
                                                    .map(ReplayBean::id)
                                                    .map(ObservableConstant::valueOf)
                                                    .orElse(ObservableConstant.valueOf(null)));
    gameColumn.setCellFactory(param -> new StringCell<>(Number::toString));
    descriptionColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().reportDescription()));
    descriptionColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    moderatorColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().lastModerator()));
    moderatorColumn.setCellFactory(param -> new StringCell<>(PlayerBean::getUsername));
    noticeColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().moderatorNotice()));
    noticeColumn.setCellFactory(param -> new WrappingStringCell<>(String::toString));
    statusColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().reportStatus()));
    statusColumn.setCellFactory(param -> new StringCell<>(status -> i18n.get(status.getI18nKey())));

    populateReportTable();
  }

  public void onReportButtonClicked() {
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

    Mono<PlayerBean> playerMono = playerService.getPlayerByName(offender.getText())
                                               .switchIfEmpty(Mono.fromRunnable(this::warnNoPlayer));

    Mono<ModerationReportBean> reportMono;
    if (!gameId.getText().isBlank()) {
      Mono<ReplayBean> replayMono = replayService.findById(Integer.parseInt(gameId.getText()))
                                                 .switchIfEmpty(Mono.fromRunnable(this::warnNoGame))
                                                 .flatMap(replay -> {
                                                   if (replay.teams()
                                                             .values()
                                                             .stream()
                                                             .flatMap(Collection::stream)
                                                             .noneMatch(
                                                                 username -> username.equals(offender.getText()))) {
                                                     return Mono.empty();
                                                   }
                                                   return Mono.just(replay);
                                                 })
                                                 .switchIfEmpty(Mono.fromRunnable(this::warnOffenderNotInGame));
      reportMono = Mono.zip(playerMono, replayMono)
                       .map(TupleUtils.function(
                           (player, replay) -> new ModerationReportBean(null, reportDescription.getText(), null,
                                                                        gameTime.getText(), null, null,
                                                                        playerService.getCurrentPlayer(),
                                                                        Set.of(player), replay, null)));
    } else {
      reportMono = playerMono.map(
          player -> new ModerationReportBean(null, reportDescription.getText(), null, gameTime.getText(), null, null,
                                             playerService.getCurrentPlayer(), Set.of(player), null, null));
    }

    reportMono.doOnNext(reportBean -> setSendingReport(true))
              .flatMap(moderationService::postModerationReport)
              .doAfterTerminate(() -> setSendingReport(false))
              .subscribe(postedReport -> {
                if (postedReport != null) {
                  populateReportTable();
                  clearReport();
                  notificationService.addImmediateInfoNotification("report.success");
                }
              }, throwable -> {
                log.error("Error submitting moderation report", throwable);
                notificationService.addImmediateErrorNotification(throwable, "report.error");
              });
  }

  private void setSendingReport(boolean sending) {
    fxApplicationThreadExecutor.execute(() -> {
      reportDialogRoot.setDisable(sending);
      reportButton.setText(i18n.get(sending ? "report.sending" : "report.submit"));
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
    log.warn(String.format("No player named %s", offender.getText()));
    notificationService.addImmediateWarnNotification("report.warning.noPlayer");
  }

  private void warnNoGameTime() {
    notificationService.addImmediateWarnNotification("report.warning.noGameTime");
  }

  private void warnNonNumericGameId() {
    log.warn("GameId {} not numeric", gameId.getText());
    notificationService.addImmediateWarnNotification("report.warning.gameIdNotNumeric");
  }

  private void warnNoGame() {
    log.warn("GameId {} does not exist", gameId.getText());
    notificationService.addImmediateWarnNotification("report.warning.noGame");
  }

  public void setOffender(PlayerBean player) {
    offender.setText(player.getUsername());
  }

  public void setOffender(String username) {
    offender.setText(username);
  }

  public void setReplay(ReplayBean replay) {
    TextFields.bindAutoCompletion(offender, replay.teams().values().stream().flatMap(Collection::stream)
        .collect(Collectors.toList()));
    gameId.setText(String.valueOf(replay.id()));
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

  private void populateReportTable() {
    moderationService.getModerationReports()
                     .collectList()
                     .map(FXCollections::observableList)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .subscribe(reportTable::setItems);
  }

  @Override
  public Pane getRoot() {
    return reportDialogRoot;
  }

  public void show() {
    Assert.checkNullIllegalState(ownerWindow, "ownerWindow must be set");

    FxStage fxStage = FxStage.create(reportDialogRoot)
        .initOwner(ownerWindow)
        .initModality(Modality.WINDOW_MODAL)
                             .withSceneFactory(themeService::createScene)
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
