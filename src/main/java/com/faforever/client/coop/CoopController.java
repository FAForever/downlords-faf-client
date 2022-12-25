package com.faforever.client.coop;

import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.game.CoopMapListController;
import com.faforever.client.game.CreateCoopGameController;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javafx.collections.FXCollections.observableList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CoopController extends AbstractViewController<Node> {

  private static final Predicate<GameBean> OPEN_COOP_GAMES_PREDICATE = gameInfoBean ->
      gameInfoBean.getStatus() == GameStatus.OPEN && gameInfoBean.getGameType() == GameType.COOP;

  private final ReplayService replayService;
  private final GameService gameService;
  private final CoopService coopService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final UiService uiService;
  private final TimeService timeService;

  public StackPane root;
  public Button selectMissionButton;
  public Pane gameViewContainer;
  public Button createGameButton;
  public Region leaderboardInfoIcon;
  public ComboBox<Integer> numberOfPlayersComboBox;
  public TableView<CoopResultBean> leaderboardTableView;
  public TableColumn<CoopResultBean, Integer> rankColumn;
  public TableColumn<CoopResultBean, Integer> playerCountColumn;
  public TableColumn<CoopResultBean, String> playerNamesColumn;
  public TableColumn<CoopResultBean, Boolean> secondaryObjectivesColumn;
  public TableColumn<CoopResultBean, OffsetDateTime> dateColumn;
  public TableColumn<CoopResultBean, Duration> timeColumn;
  public TableColumn<CoopResultBean, String> replayColumn;

  private CoopMapListController coopMapListController;
  private Popup coopMapListPopup;

  public void initialize() {
    setUpAndLoadLeaderboard();
    displayCoopGames();
  }

  private void displayCoopGames() {
      coopService.getMissions().thenAccept(coopMaps -> JavaFxUtil.runLater(() -> {
        GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
        gamesTableController.getMapPreviewColumn().setVisible(true);
        gamesTableController.getRatingRangeColumn().setVisible(false);
        gamesTableController.initializeGameTable(new FilteredList<>(gameService.getGames(), OPEN_COOP_GAMES_PREDICATE),
            mapFolderName -> coopMissionFromFolderNamer(coopMaps, mapFolderName), false);
        populateContainer(gamesTableController.getRoot());
      })).exceptionally(throwable -> {
        notificationService.addPersistentErrorNotification(throwable, "coop.couldNotLoad", throwable.getLocalizedMessage());
        return null;
      });
  }

  private void setUpAndLoadLeaderboard() {
    numberOfPlayersComboBox.setButtonCell(numberOfPlayersCell());
    numberOfPlayersComboBox.setCellFactory(param -> numberOfPlayersCell());
    numberOfPlayersComboBox.getSelectionModel().select(0);
    JavaFxUtil.addListener(numberOfPlayersComboBox.getSelectionModel().selectedItemProperty(),
        (observable, oldValue, newValue) -> loadLeaderboard(coopMapListController.getSelectedMission(), newValue));

    leaderboardTableView.setPlaceholder(new ProgressIndicator());

    // TODO don't use API object but bean instead
    rankColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getRanking()));
    rankColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerCountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPlayerCount()));
    playerCountColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerNamesColumn.setCellValueFactory(param -> new SimpleStringProperty(commaDelimitedPlayerList(param.getValue())));
    playerNamesColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    secondaryObjectivesColumn.setCellValueFactory(param -> param.getValue().secondaryObjectivesProperty());
    secondaryObjectivesColumn.setCellFactory(param -> new StringCell<>(aBoolean -> aBoolean ? i18n.get("yes") : i18n.get("no")));

    dateColumn.setCellValueFactory(param -> param.getValue().getReplay().endTimeProperty());
    dateColumn.setCellFactory(param -> new StringCell<>(timeService::asDate));

    timeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDuration()));
    timeColumn.setCellFactory(param -> new StringCell<>(timeService::shortDuration));

    replayColumn.setCellValueFactory(param -> param.getValue().getReplay().idProperty().asString());
    replayColumn.setCellFactory(param -> new NodeTableCell<>((replayId) -> {
      ReplayButtonController button = uiService.loadFxml("theme/play/coop/replay_button.fxml");
      button.setReplayId(replayId);
      button.setOnClickedAction(this::onReplayButtonClicked);
      return button.getRoot();
    }));

    Tooltip tooltip = new Tooltip(i18n.get("coop.leaderboard.table.criteria"));
    tooltip.setShowDelay(javafx.util.Duration.ZERO);
    tooltip.setShowDuration(javafx.util.Duration.seconds(30));
    Tooltip.install(leaderboardInfoIcon, tooltip);

    coopMapListController = uiService.loadFxml("theme/play/coop_map_list.fxml");
    coopMapListController.setControllerAsPopup();
    coopMapListController.setAutoSelectFirstScenarioMission(false);
    coopMapListController.setOnSelectedMissionClicked(selectedMission -> {
      if (coopMapListPopup != null && coopMapListPopup.isShowing()) {
        coopMapListPopup.hide();
      }
    });
    JavaFxUtil.addListener(coopMapListController.selectedMissionProperty(), (observable, oldValue, newValue) -> {
      if (newValue != null) {
        selectMissionButton.setText(coopMapListController.getFormattedSelectedMissionName());
        loadLeaderboard(newValue, numberOfPlayersComboBox.getSelectionModel().getSelectedItem());
      }
    });
  }

  private String coopMissionFromFolderNamer(List<CoopMissionBean> coopMaps, String mapFolderName) {
    return coopMaps.stream()
        .filter(coopMission -> coopMission.getMapFolderName().equalsIgnoreCase(mapFolderName))
        .findFirst()
        .map(CoopMissionBean::getName)
        .orElse(i18n.get("coop.unknownMission"));
  }

  private String commaDelimitedPlayerList(CoopResultBean coopResult) {
    return coopResult.getReplay().getTeams().values().stream()
        .flatMap(List::stream)
        .collect(Collectors.joining(i18n.get("textSeparator")));
  }

  private void onReplayButtonClicked(ReplayButtonController button) {
    String replayId = button.getReplayId();
    replayService.runReplay(Integer.valueOf(replayId));
  }

  private ListCell<Integer> numberOfPlayersCell() {
    return new StringListCell<>(numberOfPlayers -> switch (numberOfPlayers) {
      case 0 -> i18n.get("coop.leaderboard.allPlayers");
      case 1 -> i18n.get("coop.leaderboard.singlePlayer");
      default -> i18n.get("coop.leaderboard.numberOfPlayersFormat", numberOfPlayers);
    });
  }

  private void loadLeaderboard(CoopMissionBean mission, int playerCount) {
    leaderboardTableView.setItems(FXCollections.emptyObservableList());
    coopService.getLeaderboard(mission, playerCount)
        .thenApply(this::filterOnlyUniquePlayers)
        .thenAccept(coopLeaderboardEntries -> {
          AtomicInteger ranking = new AtomicInteger();
          coopLeaderboardEntries.forEach(coopResult -> coopResult.setRanking(ranking.incrementAndGet()));
          JavaFxUtil.runLater(() -> leaderboardTableView.setItems(observableList(coopLeaderboardEntries)));
        })
        .exceptionally(throwable -> {
          log.warn("Could not load coop leaderboard", throwable);
          notificationService.addImmediateErrorNotification(throwable, "coop.leaderboard.couldNotLoad");
          return null;
        });
  }

  private List<CoopResultBean> filterOnlyUniquePlayers(List<CoopResultBean> result) {
    Set<Set<String>> uniquePlayerNames = new HashSet<>();
    result.removeIf(coopResult -> !uniquePlayerNames.add(getAllPlayerNamesFromTeams(coopResult)));
    return result;
  }

  private Set<String> getAllPlayerNamesFromTeams(CoopResultBean coopResult) {
    return coopResult.getReplay().getTeams().values().stream().flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
  }

  private void populateContainer(Node root) {
    JavaFxUtil.assertApplicationThread();
    gameViewContainer.getChildren().setAll(root);
    JavaFxUtil.setAnchors(root, 0d);
  }

  public void onCreateGameButtonClicked() {
    CreateCoopGameController createCoopGameController = uiService.loadFxml("theme/play/create_game.fxml", CreateCoopGameController.class);
    Pane root = createCoopGameController.getRoot();
    Dialog dialog = uiService.showInDialog(this.root, root, i18n.get("games.create"));
    createCoopGameController.setOnCloseControllerRequest(dialog::close);
    root.requestFocus();
  }

  public void onSelectMissionButtonClicked() {
    if (coopMapListPopup == null) {
      coopMapListPopup = PopupUtil.createPopup(AnchorLocation.CONTENT_TOP_RIGHT, coopMapListController.getRoot());
    }

    if (coopMapListPopup.isShowing()) {
      coopMapListPopup.hide();
    } else {
      Bounds screenBounds = selectMissionButton.localToScreen(selectMissionButton.getBoundsInLocal());
      coopMapListPopup.show(selectMissionButton.getScene().getWindow(), screenBounds.getMinX() - 10, screenBounds.getMinY());
    }
  }

  public Node getRoot() {
    return root;
  }
}
