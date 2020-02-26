package com.faforever.client.coop;

import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.Player;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.faforever.client.game.KnownFeaturedMod.COOP;
import static java.util.Collections.emptySet;
import static javafx.collections.FXCollections.observableList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CoopController extends AbstractViewController<Node> {

  private static final Predicate<Game> OPEN_COOP_GAMES_PREDICATE = gameInfoBean ->
      gameInfoBean.getStatus() == GameStatus.OPEN
          && COOP.getTechnicalName().equals(gameInfoBean.getFeaturedMod());

  private final ReplayService replayService;
  private final GameService gameService;
  private final CoopService coopService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final MapService mapService;
  private final UiService uiService;
  private final TimeService timeService;
  private final WebViewConfigurer webViewConfigurer;
  private final ModService modService;

  public Node coopRoot;
  public ComboBox<CoopMission> missionComboBox;
  public ImageView mapImageView;
  public WebView descriptionWebView;
  public Pane gameViewContainer;
  public TextField titleTextField;
  public Button playButton;
  public PasswordField passwordTextField;
  public TableView<CoopResult> leaderboardTable;
  public ComboBox<Integer> numberOfPlayersComboBox;
  public TableColumn<CoopResult, Integer> rankColumn;
  public TableColumn<CoopResult, Integer> playerCountColumn;
  public TableColumn<CoopResult, String> playerNamesColumn;
  public TableColumn<CoopResult, Boolean> secondaryObjectivesColumn;
  public TableColumn<CoopResult, Duration> timeColumn;
  public TableColumn<CoopResult, String> replayColumn;

  public void initialize() {
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

    playerNamesColumn.setCellValueFactory(param -> new SimpleStringProperty(commaDelimitedPlayerList(param.getValue())));

    playerNamesColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    secondaryObjectivesColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isSecondaryObjectives()));
    secondaryObjectivesColumn.setCellFactory(param -> new StringCell<>(aBoolean -> aBoolean ? i18n.get("yes") : i18n.get("no")));

    timeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDuration()));
    timeColumn.setCellFactory(param -> new StringCell<>(timeService::shortDuration));

    replayColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getGame().getId()));
    replayColumn.setCellFactory(param -> new NodeTableCell<>((replayId) -> {
      ReplayButtonController button = uiService.loadFxml("theme/play/coop/replay_button.fxml");
      button.setReplayId(replayId);
      button.setOnClickedAction(this::onReplayButtonClicked);
      return button.getRoot();
    }));

    // Without this and no coop missions, the WebView is empty and the transparent background can't be applied to <html>
    descriptionWebView.getEngine().loadContent("<html></html>");
    webViewConfigurer.configureWebView(descriptionWebView);

    ObservableList<Game> games = gameService.getGames();

    FilteredList<Game> filteredItems = new FilteredList<>(games);
    filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE);

    coopService.getMissions().thenAccept(coopMaps -> {
      Platform.runLater(() -> {
        missionComboBox.setItems(observableList(coopMaps));
        GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");

        Function<String, String> coopMissionNameProvider = (mapFolderName -> coopMissionFromFolderNamer(coopMaps, mapFolderName));

        gamesTableController.initializeGameTable(filteredItems, coopMissionNameProvider, false);

        Node root = gamesTableController.getRoot();
        populateContainer(root);
      });

      SingleSelectionModel<CoopMission> selectionModel = missionComboBox.getSelectionModel();
      if (selectionModel.isEmpty()) {
        Platform.runLater(selectionModel::selectFirst);
      }
    }).exceptionally(throwable -> {
      notificationService.addPersistentErrorNotification(throwable, "coop.couldNotLoad", throwable.getLocalizedMessage());
      return null;
    });
  }

  private String coopMissionFromFolderNamer(List<CoopMission> coopMaps, String mapFolderName) {
    Optional<CoopMission> first = coopMaps.stream()
        .filter(coopMission -> coopMission.getMapFolderName().equalsIgnoreCase(mapFolderName))
        .findFirst();
    if (first.isPresent()) {
      return first.get().getName();
    }
    log.warn("No coop mission found for folder name: {}", mapFolderName);
    return i18n.get("coop.unknownMission");
  }

  private String commaDelimitedPlayerList(CoopResult coopResult) {
    return coopResult.getGame().getPlayerStats().stream()
        .map(GamePlayerStats::getPlayer)
        .map(Player::getLogin)
        .collect(Collectors.joining(i18n.get("textSeparator")));
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

  private ListCell<CoopMission> missionListCell() {
    return new StringListCell<>(CoopMission::getName,
        mission -> {
          Text text = new Text();
          text.getStyleClass().add(UiService.CSS_CLASS_ICON);
          switch (mission.getCategory()) {
            case AEON:
              text.setText("\uE900");
              break;
            case CYBRAN:
              text.setText("\uE902");
              break;
            case UEF:
              text.setText("\uE904");
              break;
            default:
              return null;
          }
          return text;
        }, Pos.CENTER_LEFT, "coop-mission");
  }

  private void loadLeaderboard() {
    coopService.getLeaderboard(getSelectedMission(), numberOfPlayersComboBox.getSelectionModel().getSelectedItem())
        .thenAccept(coopLeaderboardEntries -> {
          AtomicInteger ranking = new AtomicInteger();
          coopLeaderboardEntries.forEach(coopResult -> coopResult.setRanking(ranking.incrementAndGet()));
          Platform.runLater(() -> leaderboardTable.setItems(observableList(coopLeaderboardEntries)));
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"), i18n.get("coop.leaderboard.couldNotLoad"), throwable, i18n, reportingService
          ));
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

  private void populateContainer(Node root) {
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
