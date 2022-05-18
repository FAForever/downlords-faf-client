package com.faforever.client.coop;

import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.base.Strings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.faforever.client.game.KnownFeaturedMod.COOP;
import static java.util.Collections.emptySet;
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
  private final WebViewConfigurer webViewConfigurer;
  private final ModService modService;

  public GridPane coopRoot;
  public ComboBox<CoopMissionBean> missionComboBox;
  public WebView descriptionWebView;
  public Pane gameViewContainer;
  public TextField titleTextField;
  public Button playButton;
  public PasswordField passwordTextField;
  public TableView<CoopResultBean> leaderboardTable;
  public ComboBox<Integer> numberOfPlayersComboBox;
  public TableColumn<CoopResultBean, Integer> rankColumn;
  public TableColumn<CoopResultBean, Integer> playerCountColumn;
  public TableColumn<CoopResultBean, String> playerNamesColumn;
  public TableColumn<CoopResultBean, Boolean> secondaryObjectivesColumn;
  public TableColumn<CoopResultBean, OffsetDateTime> dateColumn;
  public TableColumn<CoopResultBean, Duration> timeColumn;
  public TableColumn<CoopResultBean, String> replayColumn;

  public void initialize() {
    missionComboBox.setCellFactory(param -> missionListCell());
    missionComboBox.setButtonCell(missionListCell());
    missionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSelectedMission(newValue));
    playButton.disableProperty().bind(titleTextField.textProperty().isEmpty());

    numberOfPlayersComboBox.setButtonCell(numberOfPlayersCell());
    numberOfPlayersComboBox.setCellFactory(param -> numberOfPlayersCell());
    numberOfPlayersComboBox.getSelectionModel().select(0);
    numberOfPlayersComboBox.getSelectionModel().selectedItemProperty().addListener(observable -> loadLeaderboard());

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

    // Without this and no coop missions, the WebView is empty and the transparent background can't be applied to <html>
    descriptionWebView.getEngine().loadContent("<html></html>");
    webViewConfigurer.configureWebView(descriptionWebView);

    ObservableList<GameBean> games = gameService.getGames();

    FilteredList<GameBean> filteredItems = new FilteredList<>(games);
    filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE);

    coopService.getMissions().thenAccept(coopMaps -> {
      JavaFxUtil.runLater(() -> {
        missionComboBox.setItems(observableList(coopMaps));
        GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
        gamesTableController.getMapPreviewColumn().setVisible(false); // remove it when map preview images from API will be fixed
        gamesTableController.getRatingRangeColumn().setVisible(false);

        Function<String, String> coopMissionNameProvider = (mapFolderName -> coopMissionFromFolderNamer(coopMaps, mapFolderName));

        gamesTableController.initializeGameTable(filteredItems, coopMissionNameProvider, false);

        Node root = gamesTableController.getRoot();
        populateContainer(root);
      });

      SingleSelectionModel<CoopMissionBean> selectionModel = missionComboBox.getSelectionModel();
      if (selectionModel.isEmpty()) {
        JavaFxUtil.runLater(selectionModel::selectFirst);
      }
    }).exceptionally(throwable -> {
      notificationService.addPersistentErrorNotification(throwable, "coop.couldNotLoad", throwable.getLocalizedMessage());
      return null;
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
    return String.join(", ", coopResult.getReplay().getTeams().values().stream().flatMap(List::stream).toList());
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
    });
  }

  private ListCell<CoopMissionBean> missionListCell() {
    return new StringListCell<>(CoopMissionBean::getName,
        mission -> {
          Label label = new Label();
          Region iconRegion = new Region();
          label.setGraphic(iconRegion);
          iconRegion.getStyleClass().add(UiService.CSS_CLASS_ICON);
          switch (mission.getCategory()) {
            case AEON:
              iconRegion.getStyleClass().add(UiService.AEON_STYLE_CLASS);
              break;
            case CYBRAN:
              iconRegion.getStyleClass().add(UiService.CYBRAN_STYLE_CLASS);
              break;
            case UEF:
              iconRegion.getStyleClass().add(UiService.UEF_STYLE_CLASS);
              break;
            default:
              return null;
          }
          return label;
        }, Pos.CENTER_LEFT, "coop-mission");
  }

  private void loadLeaderboard() {
    coopService.getLeaderboard(getSelectedMission(), numberOfPlayersComboBox.getSelectionModel().getSelectedItem())
        .thenApply(this::filterOnlyUniquePlayers)
        .thenAccept(coopLeaderboardEntries -> {
          AtomicInteger ranking = new AtomicInteger();
          coopLeaderboardEntries.forEach(coopResult -> coopResult.setRanking(ranking.incrementAndGet()));
          JavaFxUtil.runLater(() -> leaderboardTable.setItems(observableList(coopLeaderboardEntries)));
        })
        .exceptionally(throwable -> {
          log.warn("Could not load coop leaderboard", throwable);
          notificationService.addImmediateErrorNotification(throwable, "coop.leaderboard.couldNotLoad");
          return null;
        });
  }

  private List<CoopResultBean> filterOnlyUniquePlayers(List<CoopResultBean> result) {
    Set<String> uniquePlayerNames = new HashSet<>();
    result.removeIf(coopResult -> !uniquePlayerNames.add(commaDelimitedPlayerList(coopResult)));
    return result;
  }

  private CoopMissionBean getSelectedMission() {
    return missionComboBox.getSelectionModel().getSelectedItem();
  }

  private void setSelectedMission(CoopMissionBean mission) {
    JavaFxUtil.runLater(() -> descriptionWebView.getEngine().loadContent(mission.getDescription()));
    loadLeaderboard();
  }

  private void populateContainer(Node root) {
    JavaFxUtil.assertApplicationThread();
    gameViewContainer.getChildren().setAll(root);
    JavaFxUtil.setAnchors(root, 0d);
  }

  public void onPlayButtonClicked() {
    modService.getFeaturedMod(COOP.getTechnicalName())
        .thenAccept(featuredModBean -> gameService.hostGame(new NewGameInfo(titleTextField.getText(),
            Strings.emptyToNull(passwordTextField.getText()), featuredModBean, getSelectedMission().getMapFolderName(),
            emptySet())));
  }

  public Node getRoot() {
    return coopRoot;
  }
}
