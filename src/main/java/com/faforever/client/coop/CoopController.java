package com.faforever.client.coop;

import com.faforever.client.api.CoopLeaderboardEntry;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.faforever.client.game.KnownFeaturedMod.COOP;
import static java.util.Collections.emptySet;
import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;
import static javafx.collections.FXCollections.observableList;

public class CoopController {

  private static final Predicate<Game> OPEN_COOP_GAMES_PREDICATE = gameInfoBean ->
      gameInfoBean.getStatus() == GameState.OPEN
          && COOP.getString().equals(gameInfoBean.getFeaturedMod());

  public Node coopRoot;
  public ComboBox<CoopMission> missionComboBox;
  public ImageView mapImageView;
  public WebView descriptionWebView;
  public Pane gameViewContainer;
  public TextField titleTextField;
  public Button playButton;
  public PasswordField passwordTextField;
  public ImageView selectedGameMapView;
  public Label selectedGameTitleLabel;
  public Label selectedGameMapLabel;
  public Label selectedGameNumberOfPlayersLabel;
  public Label selectedGameHostLabel;
  public ScrollPane selectedGamePane;
  public VBox selectedGameTeamPane;
  public TableView<CoopLeaderboardEntry> leaderboardTable;
  public ComboBox<Integer> numberOfPlayersComboBox;
  public TableColumn<CoopLeaderboardEntry, Integer> rankColumn;
  public TableColumn<CoopLeaderboardEntry, Integer> playerCountColumn;
  public TableColumn<CoopLeaderboardEntry, String> playerNamesColumn;
  public TableColumn<CoopLeaderboardEntry, Boolean> secondaryObjectivesColumn;
  public TableColumn<CoopLeaderboardEntry, Integer> timeColumn;
  public TableColumn<CoopLeaderboardEntry, String> replayColumn;

  @Resource
  FxmlLoader fxmlLoader;
  @Resource
  ReplayService replayService;
  @Resource
  GameService gameService;
  @Resource
  CoopService coopService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;
  @Resource
  MapService mapService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ThemeService themeService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  TimeService timeService;
  @Resource
  WebViewConfigurer webViewConfigurer;
  @Resource
  ModService modService;

  private InvalidationListener teamsChangeListener;

  @VisibleForTesting
  Game currentGame;

  @FXML
  void initialize() {
    missionComboBox.setCellFactory(param -> missionListCell());
    missionComboBox.setButtonCell(missionListCell());
    missionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSelectedMission(newValue));
    playButton.disableProperty().bind(titleTextField.textProperty().isEmpty());

    numberOfPlayersComboBox.setButtonCell(numberOfPlayersCell());
    numberOfPlayersComboBox.setCellFactory(param -> numberOfPlayersCell());
    numberOfPlayersComboBox.getSelectionModel().select(2);
    numberOfPlayersComboBox.getSelectionModel().selectedItemProperty().addListener(observable -> loadLeaderboard());

    // TODO don't use API object but bean instead
    rankColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getRanking()));
    rankColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerCountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPlayerCount()));
    playerCountColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerNamesColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPlayerNames()));
    playerNamesColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    secondaryObjectivesColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isSecondaryObjectives()));
    secondaryObjectivesColumn.setCellFactory(param -> new StringCell<>(aBoolean -> aBoolean ? i18n.get("yes") : i18n.get("no")));

    timeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDuration()));
    timeColumn.setCellFactory(param -> new StringCell<>(seconds -> timeService.shortDuration(Duration.ofSeconds(seconds))));

    replayColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getId()));
    replayColumn.setCellFactory(param -> new NodeTableCell<>((replayId) -> {
      Button button = fxmlLoader.loadAndGetRoot("coop/replay_button.fxml");
      button.setText(replayId);
      button.setUserData(replayId);
      button.setOnAction(this::onReplayButtonClicked);
      return button;
    }));
  }

  private void onReplayButtonClicked(ActionEvent actionEvent) {
    String replayId = (String) ((Node) actionEvent.getSource()).getUserData();
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

  private ListCell<CoopMission> missionListCell() {
    ImageView iconImageView = new ImageView();
    iconImageView.setFitHeight(24);
    iconImageView.setFitWidth(24);
    return new StringListCell<>(CoopMission::getName,
        mission -> {
          switch (mission.getCategory()) {
            case AEON:
              return themeService.getThemeImage("images/aeon.png");
            case CYBRAN:
              return themeService.getThemeImage("images/cybran.png");
            case UEF:
              return themeService.getThemeImage("images/uef.png");
            default:
              return null;
          }
        }, Pos.CENTER_LEFT, iconImageView, "coop-mission");
  }

  private void loadLeaderboard() {
    coopService.getLeaderboard(getSelectedMission(), numberOfPlayersComboBox.getSelectionModel().getSelectedItem())
        .thenAccept(coopLeaderboardEntries -> Platform.runLater(() -> leaderboardTable.setItems(observableList(coopLeaderboardEntries))))
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"), i18n.get("coop.leaderboard.couldNotLoad"), Severity.ERROR, throwable,
              Arrays.asList(new ReportAction(i18n, reportingService, throwable), new DismissAction(i18n)
              )));
          return null;
        });
  }

  private CoopMission getSelectedMission() {
    return missionComboBox.getSelectionModel().getSelectedItem();
  }

  private void setSelectedMission(CoopMission mission) {
    Platform.runLater(() -> {
      descriptionWebView.getEngine().loadContent(mission.getDescription());
      mapImageView.setImage(mapService.loadPreview(mission.getMapFolderName(), PreviewSize.SMALL));
    });

    loadLeaderboard();
  }

  @PostConstruct
  void postConstruct() {
    Platform.runLater(() -> webViewConfigurer.configureWebView(descriptionWebView));

    ObservableList<Game> games = gameService.getGames();

    FilteredList<Game> filteredItems = new FilteredList<>(games);
    filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE);

    GamesTableController gamesTableController = applicationContext.getBean(GamesTableController.class);
    gamesTableController.selectedGameProperty().addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
    Platform.runLater(() -> {
      gamesTableController.initializeGameTable(filteredItems);

      Node root = gamesTableController.getRoot();
      populateContainer(root);
    });
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  public void setUpIfNecessary() {
    coopService.getMissions().thenAccept(coopMaps -> {
      Platform.runLater(() -> missionComboBox.setItems(observableList(coopMaps)));

      SingleSelectionModel<CoopMission> selectionModel = missionComboBox.getSelectionModel();
      if (selectionModel.isEmpty()) {
        Platform.runLater(selectionModel::selectFirst);
      }
    }).exceptionally(throwable -> {
      notificationService.addPersistentErrorNotification(throwable, "coop.couldNotLoad", throwable.getLocalizedMessage());
      return null;
    });
  }

  public void onPlayButtonClicked() {
    modService.getFeaturedMod(COOP.getString())
        .thenAccept(featuredModBean -> gameService.hostGame(new NewGameInfo(titleTextField.getText(),
            Strings.emptyToNull(passwordTextField.getText()), featuredModBean, getSelectedMission().getMapFolderName(),
            emptySet())));
  }

  public Node getRoot() {
    return coopRoot;
  }

  private void setSelectedGame(Game game) {
    if (game == null) {
      selectedGamePane.setVisible(false);
      return;
    }

    selectedGamePane.setVisible(true);

    selectedGameTitleLabel.textProperty().bind(game.titleProperty());

    selectedGameMapView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE),
        game.mapFolderNameProperty()
    ));

    selectedGameNumberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(),
        game.maxPlayersProperty()
    ));

    selectedGameHostLabel.textProperty().bind(game.hostProperty());
    selectedGameHostLabel.textProperty().bind(game.mapFolderNameProperty());

    if (currentGame != null) {
      currentGame.getTeams().removeListener(teamsChangeListener);
    }

    teamsChangeListener = observable -> createTeams(game.getTeams());
    teamsChangeListener.invalidated(game.getTeams());
    game.getTeams().addListener(teamsChangeListener);

    currentGame = game;
  }

  // TODO remove duplicate code
  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber) {
    selectedGameTeamPane.getChildren().clear();
    synchronized (playersByTeamNumber) {
      for (Map.Entry<? extends String, ? extends List<String>> entry : playersByTeamNumber.entrySet()) {
        TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
        teamCardController.setPlayersInTeam(entry.getKey(), entry.getValue());
        selectedGameTeamPane.getChildren().add(teamCardController.getRoot());
      }
    }
  }
}
