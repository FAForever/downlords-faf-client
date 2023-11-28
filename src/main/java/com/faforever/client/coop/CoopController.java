package com.faforever.client.coop;

import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.ControllerTableCell;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.github.benmanes.caffeine.cache.CacheLoader;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  private static final Predicate<GameBean> OPEN_COOP_GAMES_PREDICATE = gameInfoBean -> gameInfoBean.getStatus() == GameStatus.OPEN && gameInfoBean.getGameType() == GameType.COOP;

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
  private final ModService modService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final ObservableList<CoopResultBean> leaderboardUnFilteredList = FXCollections.observableArrayList();
  private final FilteredList<CoopResultBean> leaderboardFilteredList = new FilteredList<>(leaderboardUnFilteredList);

  public GridPane coopRoot;
  public ComboBox<CoopMissionBean> missionComboBox;
  public WebView descriptionWebView;
  public Pane gameViewContainer;
  public TextField titleTextField;
  public Button playButton;
  public TextField leaderboardSearchTextField;
  public PasswordField passwordTextField;
  public ImageView mapPreviewImageView;
  public Region leaderboardInfoIcon;
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
    super.initialize();
    imageViewHelper.setDefaultPlaceholderImage(mapPreviewImageView, true);

    missionComboBox.setCellFactory(param -> missionListCell());
    missionComboBox.setButtonCell(missionListCell());
    missionComboBox.getSelectionModel()
        .selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> setSelectedMission(newValue));
    playButton.disableProperty().bind(titleTextField.textProperty().isEmpty());

    numberOfPlayersComboBox.setButtonCell(numberOfPlayersCell());
    numberOfPlayersComboBox.setCellFactory(param -> numberOfPlayersCell());
    numberOfPlayersComboBox.getSelectionModel().select(0);
    numberOfPlayersComboBox.getSelectionModel().selectedItemProperty().addListener(observable -> loadLeaderboard());

    leaderboardSearchTextField.textProperty().subscribe(s -> {
      leaderboardTable.setItems(leaderboardFilteredList);
    });
    leaderboardFilteredList.predicateProperty().bind(leaderboardSearchTextField.textProperty().map(newValue -> coopResultBean -> {
      if (newValue.isEmpty()) {
        return true;
      }
      return coopResultBean.getReplay()
          .getTeams()
          .values()
          .stream()
          .flatMap(Collection::stream)
          .anyMatch(name -> name.toLowerCase().contains(newValue.toLowerCase()));
    }));

    rankColumn.setCellValueFactory(param -> param.getValue().rankingProperty().asObject());
    rankColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerCountColumn.setCellValueFactory(param -> param.getValue().playerCountProperty().asObject());
    playerCountColumn.setCellFactory(param -> new StringCell<>(String::valueOf));

    playerNamesColumn.setCellValueFactory(param -> param.getValue()
        .replayProperty()
        .flatMap(ReplayBean::teamsProperty)
        .map(teams -> teams.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.joining(i18n.get("textSeparator")))));

    playerNamesColumn.setCellFactory(param -> new StringCell<>(Function.identity()));

    secondaryObjectivesColumn.setCellValueFactory(param -> param.getValue().secondaryObjectivesProperty());
    secondaryObjectivesColumn.setCellFactory(param -> new StringCell<>(aBoolean -> aBoolean ? i18n.get("yes") : i18n.get("no")));

    dateColumn.setCellValueFactory(param -> param.getValue().getReplay().endTimeProperty());
    dateColumn.setCellFactory(param -> new StringCell<>(timeService::asDate));

    timeColumn.setCellValueFactory(param -> param.getValue().durationProperty());
    timeColumn.setCellFactory(param -> new StringCell<>(timeService::shortDuration));

    replayColumn.setCellValueFactory(param -> param.getValue().getReplay().idProperty().asString());
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

    FilteredList<GameBean> filteredItems = new FilteredList<>(gameService.getGames());
    filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE);

    coopService.getMissions().thenAccept(coopMaps -> {
      fxApplicationThreadExecutor.execute(() -> {
        missionComboBox.setItems(observableList(coopMaps));
        GamesTableController gamesTableController = uiService.loadFxml("theme/play/games_table.fxml");
        gamesTableController.getMapPreviewColumn().setVisible(false);
        gamesTableController.getRatingRangeColumn().setVisible(false);

        Function<String, String> coopMissionNameProvider = (mapFolderName -> coopMissionFromFolderNamer(coopMaps, mapFolderName));

        gamesTableController.initializeGameTable(filteredItems, coopMissionNameProvider, false);

        Node root = gamesTableController.getRoot();
        populateContainer(root);
      });

      SingleSelectionModel<CoopMissionBean> selectionModel = missionComboBox.getSelectionModel();
      if (selectionModel.isEmpty()) {
        fxApplicationThreadExecutor.execute(selectionModel::selectFirst);
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

  private ListCell<CoopMissionBean> missionListCell() {
    return new StringListCell<>(fxApplicationThreadExecutor, CoopMissionBean::getName, mission -> {
      Label label = new Label();
      Region iconRegion = new Region();
      label.setGraphic(iconRegion);
      iconRegion.getStyleClass().add(UiService.CSS_CLASS_ICON);
      switch (mission.getCategory()) {
        case AEON -> iconRegion.getStyleClass().add(UiService.AEON_STYLE_CLASS);
        case CYBRAN -> iconRegion.getStyleClass().add(UiService.CYBRAN_STYLE_CLASS);
        case UEF -> iconRegion.getStyleClass().add(UiService.UEF_STYLE_CLASS);
        default -> {
          return null;
        }
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
          fxApplicationThreadExecutor.execute(() -> {
            leaderboardUnFilteredList.setAll(coopLeaderboardEntries);
            leaderboardTable.setItems(observableList(leaderboardFilteredList));
          });
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
    return coopResult.getReplay()
        .getTeams()
        .values()
        .stream()
        .flatMap(List::stream)
        .collect(Collectors.toUnmodifiableSet());
  }


  private CoopMissionBean getSelectedMission() {
    return missionComboBox.getSelectionModel().getSelectedItem();
  }

  private void setSelectedMission(CoopMissionBean mission) {
    fxApplicationThreadExecutor.execute(() -> {
      descriptionWebView.getEngine().loadContent(mission.getDescription());
      mapPreviewImageView.setImage(mapService.loadPreview(mission.getMapFolderName(), PreviewSize.LARGE));
    });
    loadLeaderboard();
  }

  private void populateContainer(Node root) {
    JavaFxUtil.assertApplicationThread();
    gameViewContainer.getChildren().setAll(root);
    JavaFxUtil.setAnchors(root, 0d);
  }

  public void onPlayButtonClicked() {
    modService.getFeaturedMod(COOP.getTechnicalName())
        .toFuture()
        .thenAccept(featuredModBean -> gameService.hostGame(new NewGameInfo(titleTextField.getText(), Strings.emptyToNull(passwordTextField.getText()), featuredModBean, getSelectedMission().getMapFolderName(), emptySet())))
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.error("Could not host coop game", throwable);
          if (throwable instanceof NotifiableException notifiableException) {
            notificationService.addErrorNotification(notifiableException);
          } else {
            notificationService.addImmediateErrorNotification(throwable, "coop.host.error");
          }
          return null;
        });
  }

  public void onMapPreviewImageClicked() {
    Optional.ofNullable(mapPreviewImageView.getImage()).ifPresent(PopupUtil::showImagePopup);
  }

  public Node getRoot() {
    return coopRoot;
  }
}
