package com.faforever.client.coop;

import com.faforever.client.domain.api.CoopMission;
import com.faforever.client.domain.api.CoopResult;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.fx.ControllerTableCell;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.ObservableConstant;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.base.Strings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.faforever.client.game.KnownFeaturedMod.COOP;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CoopController extends NodeController<Node> {

  private static final Predicate<GameInfo> OPEN_COOP_GAMES_PREDICATE = gameInfoBean -> gameInfoBean.getStatus() == GameStatus.OPEN && gameInfoBean.getGameType() == GameType.COOP;

  private final GameRunner gameRunner;
  private final ReplayService replayService;
  private final GameService gameService;
  private final CoopService coopService;
  private final ImageViewHelper imageViewHelper;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final UiService uiService;
  private final TimeService timeService;
  private final MapService mapService;
  private final WebViewConfigurer webViewConfigurer;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final ObservableList<CoopResult> leaderboardUnFilteredList = FXCollections.observableArrayList();
  private final FilteredList<CoopResult> leaderboardFilteredList = new FilteredList<>(leaderboardUnFilteredList);

  public GridPane coopRoot;
  public ComboBox<CoopMission> missionComboBox;
  public WebView descriptionWebView;
  public Pane gameViewContainer;
  public TextField titleTextField;
  public Button playButton;
  public TextField leaderboardSearchTextField;
  public PasswordField passwordTextField;
  public ImageView mapPreviewImageView;
  public Region leaderboardInfoIcon;
  public TableView<CoopResult> leaderboardTable;
  public ComboBox<Integer> numberOfPlayersComboBox;
  public TableColumn<CoopResult, Number> rankColumn;
  public TableColumn<CoopResult, Number> playerCountColumn;
  public TableColumn<CoopResult, String> playerNamesColumn;
  public TableColumn<CoopResult, Boolean> secondaryObjectivesColumn;
  public TableColumn<CoopResult, OffsetDateTime> dateColumn;
  public TableColumn<CoopResult, Duration> timeColumn;
  public TableColumn<CoopResult, String> replayColumn;
  public GamesTableController gamesTableController;

  @Override
  protected void onInitialize() {
    missionComboBox.setCellFactory(param -> missionListCell());
    missionComboBox.setButtonCell(missionListCell());
    missionComboBox.getSelectionModel().selectedItemProperty().when(showing).subscribe(this::setSelectedMission);

    mapPreviewImageView.imageProperty()
                       .bind(missionComboBox.getSelectionModel().selectedItemProperty().map(CoopMission::mapFolderName)
                                            .map(folderName -> mapService.loadPreview(folderName, PreviewSize.LARGE))
                                            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                            .when(showing));
    playButton.disableProperty().bind(titleTextField.textProperty().isEmpty().when(showing));

    numberOfPlayersComboBox.setButtonCell(numberOfPlayersCell());
    numberOfPlayersComboBox.setCellFactory(param -> numberOfPlayersCell());
    numberOfPlayersComboBox.getSelectionModel().select(0);
    numberOfPlayersComboBox.getSelectionModel().selectedItemProperty().subscribe(this::loadLeaderboard);

    leaderboardFilteredList.predicateProperty()
                           .bind(leaderboardSearchTextField.textProperty().map(newValue -> coopResultBean -> {
                             if (newValue.isEmpty()) {
                               return true;
                             }
                             return coopResultBean.replay().teams()
                                                  .values()
                                                  .stream()
                                                  .flatMap(Collection::stream)
                                                  .anyMatch(
                                                      name -> name.toLowerCase().contains(newValue.toLowerCase()));
                           }));
    leaderboardTable.setItems(leaderboardFilteredList);

    rankColumn.setCellValueFactory(param -> ObservableConstant.valueOf((param.getValue().ranking())));
    rankColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerCountColumn.setCellValueFactory(param -> ObservableConstant.valueOf((param.getValue().playerCount())));
    playerCountColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerNamesColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().replay().teams().values()
                                                                           .stream()
                                                                           .flatMap(Collection::stream)
                                                                           .collect(Collectors.joining(
                                                                               i18n.get("textSeparator")))));

    playerNamesColumn.setCellFactory(param -> new StringCell<>(Function.identity()));

    secondaryObjectivesColumn.setCellValueFactory(
        param -> ObservableConstant.valueOf((param.getValue().secondaryObjectives())));
    secondaryObjectivesColumn.setCellFactory(
        param -> new StringCell<>(aBoolean -> aBoolean ? i18n.get("yes") : i18n.get("no")));

    dateColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().replay().endTime()));
    dateColumn.setCellFactory(param -> new StringCell<>(timeService::asDate));

    timeColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().duration()));
    timeColumn.setCellFactory(param -> new StringCell<>(timeService::shortDuration));

    replayColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().replay().id().toString()));
    replayColumn.setCellFactory(param -> {
      ReplayButtonController controller = uiService.loadFxml("theme/play/coop/replay_button.fxml");
      controller.setOnClickedAction(this::onReplayButtonClicked);
      return new ControllerTableCell<>(controller, ReplayButtonController::setReplayId);
    });

    Tooltip tooltip = new Tooltip(i18n.get("coop.leaderboard.table.criteria"));
    tooltip.setShowDelay(javafx.util.Duration.ZERO);
    tooltip.setShowDuration(javafx.util.Duration.seconds(30));
    Tooltip.install(leaderboardInfoIcon, tooltip);

    // Without this and no coop missions, the WebView is empty and the transparent background can't be applied to <html>
    descriptionWebView.getEngine().loadContent("<html></html>");
    webViewConfigurer.configureWebView(descriptionWebView);

    FilteredList<GameInfo> filteredItems = new FilteredList<>(gameService.getGames());
    filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE);

    coopService.getMissions()
               .collectList()
               .map(FXCollections::observableList)
               .publishOn(fxApplicationThreadExecutor.asScheduler())
               .subscribe(coopMaps -> {
                 missionComboBox.setItems(coopMaps);

                 gamesTableController.initializeGameTable(filteredItems,
                                                          mapFolderName -> coopMissionFromFolderName(coopMaps,
                                                                                                     mapFolderName),
                                                          false);
                 gamesTableController.getMapPreviewColumn().setVisible(false);
                 gamesTableController.getRatingRangeColumn().setVisible(false);


                 SingleSelectionModel<CoopMission> selectionModel = missionComboBox.getSelectionModel();
                 if (selectionModel.isEmpty()) {
                   selectionModel.selectFirst();
                 }
               }, throwable -> notificationService.addPersistentErrorNotification("coop.couldNotLoad",
                                                                                  throwable.getLocalizedMessage()));
  }

  private String coopMissionFromFolderName(List<CoopMission> coopMaps, String mapFolderName) {
    return coopMaps.stream().filter(coopMission -> coopMission.mapFolderName().equalsIgnoreCase(mapFolderName))
                   .findFirst()
                   .map(CoopMission::name)
                   .orElse(i18n.get("coop.unknownMission"));
  }

  private void onReplayButtonClicked(ReplayButtonController button) {
    String replayId = button.getReplayId();
    replayService.runReplay(Integer.valueOf(replayId));
  }

  private ListCell<Integer> numberOfPlayersCell() {
    return new StringListCell<>(numberOfPlayers -> {
      if (numberOfPlayers == 0) {
        return i18n.get("coop.leaderboard.allPlayers");
      }
      if (numberOfPlayers == 1) {
        return i18n.get("coop.leaderboard.singlePlayer");
      }
      return i18n.get("coop.leaderboard.numberOfPlayersFormat", numberOfPlayers);
    }, fxApplicationThreadExecutor);
  }

  private ListCell<CoopMission> missionListCell() {
    return new StringListCell<>(fxApplicationThreadExecutor, CoopMission::name, mission -> {
      Label label = new Label();
      Region iconRegion = new Region();
      label.setGraphic(iconRegion);
      iconRegion.getStyleClass().add(ThemeService.CSS_CLASS_ICON);
      switch (mission.category()) {
        case AEON -> iconRegion.getStyleClass().add(ThemeService.AEON_STYLE_CLASS);
        case CYBRAN -> iconRegion.getStyleClass().add(ThemeService.CYBRAN_STYLE_CLASS);
        case UEF -> iconRegion.getStyleClass().add(ThemeService.UEF_STYLE_CLASS);
        default -> {
          return null;
        }
      }
      return label;
    }, Pos.CENTER_LEFT, "coop-mission");
  }

  private void loadLeaderboard() {
    CoopMission selectedMission = getSelectedMission();
    Integer numberOfPlayers = numberOfPlayersComboBox.getSelectionModel().getSelectedItem();
    if (selectedMission == null || numberOfPlayers == null) {
      return;
    }

    coopService.getLeaderboard(selectedMission, numberOfPlayers)
               .collectList()
               .publishOn(fxApplicationThreadExecutor.asScheduler())
               .subscribe(leaderboardUnFilteredList::setAll, throwable -> {
                 log.warn("Could not load coop leaderboard", throwable);
                 notificationService.addImmediateErrorNotification(throwable, "coop.leaderboard.couldNotLoad");
               });
  }

  private Set<String> getAllPlayerNamesFromTeams(CoopResult coopResult) {
    return coopResult.replay().teams()
                     .values()
                     .stream()
                     .flatMap(List::stream)
                     .collect(Collectors.toUnmodifiableSet());
  }


  private CoopMission getSelectedMission() {
    return missionComboBox.getSelectionModel().getSelectedItem();
  }

  private void setSelectedMission(CoopMission mission) {
    if (mission == null) {
      return;
    }
    fxApplicationThreadExecutor.execute(() -> {
      String description = mission.description();
      if (description != null) {
        descriptionWebView.getEngine().loadContent(description);
      }
    });
    loadLeaderboard();
  }

  public void onPlayButtonClicked() {
    gameRunner.host(new NewGameInfo(titleTextField.getText(), Strings.emptyToNull(passwordTextField.getText()),
                                    COOP.getTechnicalName(), getSelectedMission().mapFolderName(), Set.of()));
  }

  public void onMapPreviewImageClicked() {
    Optional.ofNullable(mapPreviewImageView.getImage()).ifPresent(PopupUtil::showImagePopup);
  }

  @Override
  public Node getRoot() {
    return coopRoot;
  }
}
