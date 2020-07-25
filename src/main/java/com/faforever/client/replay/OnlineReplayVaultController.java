package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OnlineReplayVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 10;
  private static final int PAGE_SIZE = 100;

  private final ReplayService replayService;
  private final UiService uiService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final ReportingService reportingService;

  public Pane replayVaultRoot;
  public Pane newestPane;
  public Pane highestRatedPane;
  public Pane mostWatchedPane;
  public VBox searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public VBox loadingPane;
  public VBox contentPane;
  public Button backButton;
  public ScrollPane scrollPane;
  public SearchController searchController;
  public VBox ownReplaysPane;
  public Pagination pagination;
  public Button lastPageButton;
  public Button firstPageButton;

  private ReplayDetailController replayDetailController;
  private ReplaySearchType replaySearchType;
  private int playerId;
  private final ObjectProperty<State> state;

  public OnlineReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, PreferencesService preferencesService, ReportingService reportingService) {
    this.replayService = replayService;
    this.uiService = uiService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.reportingService = reportingService;

    state = new SimpleObjectProperty<>(State.UNINITIALIZED);
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    pagination.managedProperty().bind(pagination.visibleProperty());
    firstPageButton.managedProperty().bind(firstPageButton.visibleProperty());
    firstPageButton.disableProperty().bind(pagination.currentPageIndexProperty().isEqualTo(0));
    lastPageButton.managedProperty().bind(lastPageButton.visibleProperty());

    searchController.setRootType(Game.class);
    searchController.setSearchListener(this::onSearch);
    searchController.setSearchableProperties(SearchablePropertyMappings.GAME_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().onlineReplaySortConfigProperty());

    BooleanBinding inSearchableState = Bindings.createBooleanBinding(() -> state.get() != State.SEARCHING, state);
    searchController.setSearchButtonDisabledCondition(inSearchableState);
    searchController.setOnlyShowLastYearCheckBoxVisible(true, true);

    pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
          if (!oldValue.equals(newValue)) {
            SearchConfig searchConfig = searchController.getLastSearchConfig();
            onPageChange(searchConfig, newValue.intValue() + 1, false);
          }
        }
    );
    firstPageButton.setOnAction(event -> pagination.setCurrentPageIndex(0));
    lastPageButton.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getPageCount() - 1));
  }

  private void displaySearchResult(List<Replay> replays, boolean append) {
    enterSearchingState();
    populateReplays(replays, searchResultPane, append);
    enterResultState();
  }

  private void populateReplays(List<Replay> replays, Pane pane, boolean append) {
    JavaFxUtil.assertBackgroundThread();
    ObservableList<Node> children = pane.getChildren();

    List<Node> childrenToAdd = replays.stream()
        .map(replay -> {
          ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
          controller.setReplay(replay);
          controller.setOnOpenDetailListener(this::onShowReplayDetail);
          if (replays.size() == 1 && !append) {
            Platform.runLater(() -> onShowReplayDetail(replay));
          }
          return controller.getRoot();
        })
        .collect(Collectors.toList());

    Platform.runLater(() -> {
      if (!append) {
        children.clear();
      }
      children.addAll(childrenToAdd);
    });
  }

  public void populateReplays(List<Replay> replays, Pane pane) {
    populateReplays(replays, pane, false);
  }

  public void onShowReplayDetail(Replay replay) {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    replayDetailController.setReplay(replay);

    Node replayDetailRoot = replayDetailController.getRoot();
    replayDetailRoot.setVisible(true);
    replayDetailRoot.requestFocus();

    replayVaultRoot.getChildren().add(replayDetailRoot);
    AnchorPane.setTopAnchor(replayDetailRoot, 0d);
    AnchorPane.setRightAnchor(replayDetailRoot, 0d);
    AnchorPane.setBottomAnchor(replayDetailRoot, 0d);
    AnchorPane.setLeftAnchor(replayDetailRoot, 0d);
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowReplayEvent) {
      if (state.get() == State.UNINITIALIZED) {
        state.addListener(new ChangeListener<>() {
          @Override
          public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            if (newValue != State.UNINITIALIZED) {
              state.removeListener(this);
              onShowReplayEvent((ShowReplayEvent) navigateEvent);
            }
          }
        });
        loadPreselectedReplays();
      } else {
        onShowReplayEvent((ShowReplayEvent) navigateEvent);
      }
    } else if (navigateEvent instanceof ShowUserReplaysEvent) {
      onShowUserReplaysEvent((ShowUserReplaysEvent) navigateEvent);
    } else if (state.get() == State.UNINITIALIZED) {
      loadPreselectedReplays();
    }
  }

  private void onShowReplayEvent(ShowReplayEvent event) {
    int replayId = event.getReplayId();
    replayService.findById(replayId).thenAccept(replay -> {
      if (replay.isPresent()) {
        Platform.runLater(() -> onShowReplayDetail(replay.get()));
      } else {
        notificationService.addNotification(new ImmediateNotification(i18n.get("replay.notFoundTitle"), i18n.get("replay.replayNotFoundText", replayId), Severity.WARN));
      }
    });
  }

  private void onShowUserReplaysEvent(ShowUserReplaysEvent event) {
    enterSearchingState();
    replaySearchType = ReplaySearchType.PLAYER;
    playerId = event.getPlayerId();
    SortConfig sortConfig = new SortConfig("startTime", SortOrder.DESC);
    displayReplaysFromSupplier(() -> replayService.getReplaysForPlayerWithPageCount(playerId, PAGE_SIZE, 1, sortConfig), true);
  }


  @Override
  public Node getRoot() {
    return replayVaultRoot;
  }

  private void enterSearchingState() {
    state.set(State.SEARCHING);

    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
    backButton.setVisible(false);
    pagination.setVisible(false);
    firstPageButton.setVisible(false);
    lastPageButton.setVisible(false);
  }

  private void enterResultState() {
    state.set(State.RESULT);

    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
    backButton.setVisible(true);
    pagination.setVisible(true);
    firstPageButton.setVisible(true);
    lastPageButton.setVisible(true);
  }

  private void enterShowRoomState() {
    state.set(State.SHOWROOM);

    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
    backButton.setVisible(false);
    pagination.setVisible(false);
    firstPageButton.setVisible(false);
    lastPageButton.setVisible(false);
  }

  private void onSearch(SearchConfig searchConfig) {
    replaySearchType = ReplaySearchType.SEARCH;
    onFirstPageOpened(searchConfig);
  }

  private void onPageChange(SearchConfig searchConfig, int page, boolean firstLoad) {
    enterSearchingState();
    switch (replaySearchType) {
      case SEARCH:
        displayReplaysFromSupplier(() -> replayService.findByQueryWithPageCount(searchConfig.getSearchQuery(), PAGE_SIZE, page, searchConfig.getSortConfig()), firstLoad);
        break;
      case OWN:
        displayReplaysFromSupplier(() -> replayService.getOwnReplaysWithPageCount(PAGE_SIZE, page), firstLoad);
        break;
      case NEWEST:
        displayReplaysFromSupplier(() -> replayService.getNewestReplaysWithPageCount(PAGE_SIZE, page), firstLoad);
        break;
      case HIGHEST_RATED:
        displayReplaysFromSupplier(() -> replayService.getHighestRatedReplaysWithPageCount(PAGE_SIZE, page), firstLoad);
        break;
      case PLAYER:
        displayReplaysFromSupplier(() -> replayService.getReplaysForPlayerWithPageCount(playerId, PAGE_SIZE, page, new SortConfig("startTime", SortOrder.DESC)), firstLoad);
        break;
    }
  }

  private void onFirstPageOpened(SearchConfig searchConfig) {
    onPageChange(searchConfig, 1, true);
    if (pagination.getCurrentPageIndex() != 0) {
      pagination.setCurrentPageIndex(0);
    }
  }

  private void displaySearchResult(List<Replay> replays) {
    displaySearchResult(replays, false);
  }

  public void onBackButtonClicked() {
    loadPreselectedReplays();
  }

  public void onRefreshButtonClicked() {
    if (pagination.isVisible()) {
      onPageChange(searchController.getLastSearchConfig(), pagination.currentPageIndexProperty().getValue() + 1, false);
    } else {
      loadPreselectedReplays();
    }
  }

  private void loadPreselectedReplays() {
    enterSearchingState();
    replayService.getNewestReplaysWithPageCount(TOP_ELEMENT_COUNT, 1)
        .thenAccept(replays -> populateReplays(replays.getFirst(), newestPane))
        .thenCompose(aVoid -> replayService.getHighestRatedReplaysWithPageCount(TOP_ELEMENT_COUNT, 1).thenAccept(highestRatedReplays -> populateReplays(highestRatedReplays.getFirst(), highestRatedPane)))
        .thenCompose(aVoid -> replayService.getOwnReplaysWithPageCount(TOP_ELEMENT_COUNT, 1).thenAccept(highestRatedReplays -> populateReplays(highestRatedReplays.getFirst(), ownReplaysPane)))
        .thenRun(this::enterShowRoomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate replays", throwable);
          return null;
        });
  }

  public void onMoreNewestButtonClicked() {
    replaySearchType = ReplaySearchType.NEWEST;
    onFirstPageOpened(null);
  }

  public void onMoreHighestRatedButtonClicked() {
    replaySearchType = ReplaySearchType.HIGHEST_RATED;
    onFirstPageOpened(null);
  }

  private void displayReplaysFromSupplier(Supplier<CompletableFuture<Tuple<List<Replay>, Integer>>> mapsSupplier, boolean firstLoad) {
    mapsSupplier.get()
        .thenAccept(tuple -> {
          displaySearchResult(tuple.getFirst());
          if (firstLoad) {
            //when theres no search results the page count should be 1, 0 (which is returned) results in infinite pages
            Platform.runLater(() -> pagination.setPageCount(Math.max(1, tuple.getSecond())));
          }
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"), i18n.get("vault.replays.searchError"), throwable, i18n, reportingService
          ));
          enterResultState();
          return null;
        });
  }

  public void onMoreOwnButtonClicked() {
    replaySearchType = ReplaySearchType.OWN;
    onFirstPageOpened(null);
  }

  private enum ReplaySearchType {
    SEARCH, OWN, NEWEST, HIGHEST_RATED, PLAYER
  }

  private enum State {
    SEARCHING, RESULT, UNINITIALIZED, SHOWROOM
  }
}
