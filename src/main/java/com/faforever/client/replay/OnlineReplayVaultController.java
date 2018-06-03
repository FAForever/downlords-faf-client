package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OnlineReplayVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 10;
  private static final int TOP_MORE_ELEMENT_COUNT = 100;
  private static final int MAX_SEARCH_RESULTS = 100;

  private final ReplayService replayService;
  private final UiService uiService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PreferencesService preferencesService;

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
  public Button moreButton;

  private ReplayDetailController replayDetailController;
  private int currentPage;
  private Supplier<CompletableFuture<List<Replay>>> currentSupplier;
  private final ObjectProperty<State> state;

  @Inject
  public OnlineReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, PreferencesService preferencesService) {
    this.replayService = replayService;
    this.uiService = uiService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.preferencesService = preferencesService;

    state = new SimpleObjectProperty<>(State.UNINITIALIZED);
  }

  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    moreButton.managedProperty().bind(moreButton.visibleProperty());

    searchController.setRootType(Game.class);
    searchController.setSearchListener(this::onSearch);
    searchController.setSearchableProperties(SearchableProperties.GAME_PROPERTIES);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().onlineReplaySortConfigProperty());
  }

  private void displaySearchResult(List<Replay> replays, boolean append) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
    backButton.setVisible(true);
    populateReplays(replays, searchResultPane, append);
    moreButton.setVisible(replays.size() == MAX_SEARCH_RESULTS);
  }

  private void populateReplays(List<Replay> replays, Pane pane, boolean append) {
    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(() -> {
      if (!append) {
        children.clear();
      }
      replays.forEach(replay -> {
        ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
        controller.setReplay(replay);
        controller.setOnOpenDetailListener(this::onShowReplayDetail);
        children.add(controller.getRoot());

        if (replays.size() == 1 && !append) {
          onShowReplayDetail(replay);
        }
      });
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
  public void onDisplay(NavigateEvent navigateEvent) {
    super.onDisplay(navigateEvent);
    if (state.get() == State.UNINITIALIZED) {
      if (navigateEvent instanceof ShowReplayEvent) {
        state.addListener(new ChangeListener<State>() {
          @Override
          public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            if (newValue != State.UNINITIALIZED) {
              Platform.runLater(() -> onShowReplayDetail(((ShowReplayEvent) navigateEvent).getReplay()));
              state.removeListener(this);
            }
          }
        });
      }
      refresh();
      return;
    }
    if (navigateEvent instanceof ShowReplayEvent) {
      onShowReplayDetail(((ShowReplayEvent) navigateEvent).getReplay());
    }
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
    moreButton.setVisible(false);
  }

  private void enterResultState() {
    state.set(State.RESULT);

    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
    backButton.setVisible(false);
    moreButton.setVisible(false);
  }

  private void onSearch(SearchConfig searchConfig) {
    enterSearchingState();
    displayReplaysFromSupplier(() -> replayService.findByQuery(searchConfig.getSearchQuery(), MAX_SEARCH_RESULTS, currentPage++, searchConfig.getSortConfig()));
  }

  private void displaySearchResult(List<Replay> replays) {
    displaySearchResult(replays, false);
  }

  public void onBackButtonClicked() {
    enterResultState();
  }

  public void onRefreshButtonClicked() {
    refresh();
  }

  private void refresh() {
    enterSearchingState();
    replayService.getNewestReplays(TOP_ELEMENT_COUNT, 1)
        .thenAccept(replays -> populateReplays(replays, newestPane))
        .thenCompose(aVoid -> replayService.getHighestRatedReplays(TOP_ELEMENT_COUNT, 1).thenAccept(modInfoBeans -> populateReplays(modInfoBeans, highestRatedPane)))
        .thenRun(this::enterResultState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate replays", throwable);
          return null;
        });
  }

  public void onMoreNewestButtonClicked() {
    enterSearchingState();
    displayReplaysFromSupplier(() -> replayService.getNewestReplays(TOP_MORE_ELEMENT_COUNT, currentPage++));
  }

  public void onMoreHighestRatedButtonClicked() {
    enterSearchingState();
    displayReplaysFromSupplier(() -> replayService.getHighestRatedReplays(TOP_MORE_ELEMENT_COUNT, currentPage++));
  }

  public void onLoadMoreButtonClicked(ActionEvent actionEvent) {
    currentSupplier.get()
        .thenAccept(replays -> displaySearchResult(replays, true));
  }

  private void displayReplaysFromSupplier(Supplier<CompletableFuture<List<Replay>>> mapsSupplier) {
    currentPage = 1;
    this.currentSupplier = mapsSupplier;
    mapsSupplier.get()
        .thenAccept(this::displaySearchResult)
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.replays.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          enterResultState();
          return null;
        });
  }

  private enum State {
    SEARCHING, RESULT, UNINITIALIZED
  }
}
