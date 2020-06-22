package com.faforever.client.map;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO I'd like to avoid the additional "getMost*" methods and always use the map query function instead, however,
// this is currently not viable since Elide can't yet sort by relationship attributes. Once it supports that
// (see https://github.com/yahoo/elide/issues/353), this can be refactored.
public class MapVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @VisibleForTesting
  static final int TOP_ELEMENT_COUNT = 7;
  @VisibleForTesting
  static final int LOAD_PER_PAGE = 50;
  /**
   * How many mod cards should be badged into one UI thread runnable.
   */
  private static final int BATCH_SIZE = 10;

  private final MapService mapService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final NotificationService notificationService;
  private final ObjectProperty<State> state;
  private final ReportingService reportingService;
  private final PlayerService playerService;
  public Pane searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public Node loadingLabel;
  public Pane newestPane;
  public Pane mostPlayedPane;
  public Pane mostLikedPane;
  public Pane recommendedPane;
  public Button moreNewestButton;
  public Button moreMostPlayedButton;
  public Button moreMostLikedButton;
  public Button moreRecommendedButton;
  public Button moreLadderButton;
  public StackPane mapVaultRoot;
  public ScrollPane scrollPane;
  public Button backButton;
  public SearchController searchController;
  public FlowPane ladderPane;
  public FlowPane ownedPane;
  public Label ownedMoreLabel;
  public Button moreOwnedButton;
  public Button previousButton;
  public Button nextButton;
  public Label currentPageLabel;
  public HBox paginationHBox;
  private MapDetailController mapDetailController;
  private int currentPage;
  private Supplier<CompletableFuture<List<MapBean>>> currentSupplier;

  public MapVaultController(MapService mapService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                            UiService uiService, NotificationService notificationService, ReportingService reportingService,
                            PlayerService playerService) {
    this.mapService = mapService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.playerService = playerService;

    state = new SimpleObjectProperty<>(State.UNINITIALIZED);
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);

    loadingLabel.managedProperty().bind(loadingLabel.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    paginationHBox.managedProperty().bind(paginationHBox.visibleProperty());

    mapDetailController = uiService.loadFxml("theme/vault/map/map_detail.fxml");
    Node mapDetailRoot = mapDetailController.getRoot();
    mapVaultRoot.getChildren().add(mapDetailRoot);
    AnchorPane.setTopAnchor(mapDetailRoot, 0d);
    AnchorPane.setRightAnchor(mapDetailRoot, 0d);
    AnchorPane.setBottomAnchor(mapDetailRoot, 0d);
    AnchorPane.setLeftAnchor(mapDetailRoot, 0d);
    mapDetailRoot.setVisible(false);

    eventBus.register(this);

    searchController.setRootType(com.faforever.client.api.dto.Map.class);
    searchController.setSearchListener(this::searchByQuery);
    searchController.setSearchableProperties(SearchablePropertyMappings.MAP_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().mapSortConfigProperty());

    BooleanBinding inSearchableState = Bindings.createBooleanBinding(() -> state.get() != State.LOADING, state);
    searchController.setSearchButtonDisabledCondition(inSearchableState);
  }

  private void searchByQuery(SearchConfig searchConfig) {
    SearchConfig newSearchConfig = new SearchConfig(searchConfig.getSortConfig(), searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"");
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.findByQuery(newSearchConfig, currentPage, LOAD_PER_PAGE));
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowLadderMapsEvent) {
      showMoreLadderdMaps();
    } else if (state.get() == State.UNINITIALIZED) {
      displayShowroomMaps();
    }
  }

  private void displayShowroomMaps() {
    enterLoadingState();
    mapService.getMostPlayedMaps(TOP_ELEMENT_COUNT, 1).thenAccept(maps -> replaceSearchResult(maps, mostPlayedPane))
        .thenCompose(aVoid -> mapService.getRecommendedMaps(TOP_ELEMENT_COUNT, 1)).thenAccept(maps -> replaceSearchResult(maps, recommendedPane))
        .thenCompose(aVoid -> mapService.getHighestRatedMaps(TOP_ELEMENT_COUNT, 1)).thenAccept(maps -> replaceSearchResult(maps, mostLikedPane))
        .thenCompose(aVoid -> mapService.getNewestMaps(TOP_ELEMENT_COUNT, 1)).thenAccept(maps -> replaceSearchResult(maps, newestPane))
        .thenCompose(aVoid -> mapService.getLadderMaps(TOP_ELEMENT_COUNT, 1).thenAccept(maps -> replaceSearchResult(maps, ladderPane)))
        .thenCompose(aVoid -> {
          Player player = playerService.getCurrentPlayer()
              .orElseThrow(() -> new IllegalStateException("Current player not set"));
          return mapService.getOwnedMaps(player.getId(), TOP_ELEMENT_COUNT, 1);
        })
        .thenAccept(mapBeans -> {
          if (mapBeans.isEmpty()) {
            hideOwned();
          }
          replaceSearchResult(mapBeans, ownedPane);
        })
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate maps", throwable);
          return null;
        });
  }

  private void hideOwned() {
    Platform.runLater(() -> {
      ownedPane.setVisible(false);
      moreOwnedButton.setVisible(false);
      ownedMoreLabel.setVisible(false);
    });
  }

  private void enterLoadingState() {
    Platform.runLater(() -> {
      state.set(State.LOADING);
      showroomGroup.setVisible(false);
      searchResultGroup.setVisible(false);
      loadingLabel.setVisible(true);
      backButton.setVisible(true);
      paginationHBox.setVisible(false);
    });
  }

  private void enterSearchResultState() {
    Platform.runLater(() -> {
      state.set(State.SEARCH_RESULT);
      showroomGroup.setVisible(false);
      searchResultGroup.setVisible(true);
      loadingLabel.setVisible(false);
      backButton.setVisible(true);
      paginationHBox.setVisible(true);
      nextButton.setDisable(searchResultPane.getChildren().size() < LOAD_PER_PAGE);
      previousButton.setDisable(currentPage <= 1);
      currentPageLabel.setText(i18n.get("vault.currentPage", currentPage));
    });
  }

  private void enterShowroomState() {
    Platform.runLater(() -> {
      state.set(State.SHOWROOM);
      showroomGroup.setVisible(true);
      searchResultGroup.setVisible(false);
      loadingLabel.setVisible(false);
      backButton.setVisible(false);
      paginationHBox.setVisible(false);
    });
  }

  private void onShowMapDetail(MapBean map) {
    JavaFxUtil.assertApplicationThread();
    mapDetailController.setMap(map);
    mapDetailController.getRoot().setVisible(true);
    mapDetailController.getRoot().requestFocus();
  }

  public void onUploadMapButtonClicked() {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("mapVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openMapUploadWindow(result.toPath());
    });
  }

  public Node getRoot() {
    return mapVaultRoot;
  }

  private void openMapUploadWindow(Path path) {
    MapUploadController mapUploadController = uiService.loadFxml("theme/vault/map/map_upload.fxml");
    mapUploadController.setMapPath(path);

    Node root = mapUploadController.getRoot();
    Dialog dialog = uiService.showInDialog(mapVaultRoot, root, i18n.get("mapVault.upload.title"));
    mapUploadController.setOnCancelButtonClickedListener(dialog::close);
  }

  public void onRefreshButtonClicked() {
    mapService.evictCache();
    switch (state.get()) {
      case SHOWROOM:
        displayShowroomMaps();
        break;
      case SEARCH_RESULT:
        currentSupplier.get()
            .thenAccept(this::displayMaps)
            .exceptionally(throwable -> {
              notificationService.addNotification(new ImmediateErrorNotification(
                  i18n.get("errorTitle"), i18n.get("vault.mods.searchError"),
                  throwable, i18n, reportingService
              ));
              enterShowroomState();
              return null;
            });
        break;
      default:
        // Do nothing
    }
  }

  public void onBackButtonClicked() {
    enterShowroomState();
  }

  @Subscribe
  public void onMapUploaded(MapUploadedEvent event) {
    onRefreshButtonClicked();
  }

  public void showMoreRecommendedMaps() {
    enterLoadingState();
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getRecommendedMaps(LOAD_PER_PAGE, currentPage));
  }

  public void showMoreHighestRatedMaps() {
    enterLoadingState();
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getHighestRatedMaps(LOAD_PER_PAGE, currentPage));
  }

  public void showMoreMostRecentMaps() {
    enterLoadingState();
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getNewestMaps(LOAD_PER_PAGE, currentPage));
  }

  public void showMoreMostPlayedMaps() {
    enterLoadingState();
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getMostPlayedMaps(LOAD_PER_PAGE, currentPage));
  }

  public void showMoreLadderdMaps() {
    enterLoadingState();
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getLadderMaps(LOAD_PER_PAGE, currentPage));
  }

  public void showMoreOwnedMaps() {
    enterLoadingState();
    Player currentPlayer = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("Current player was null"));
    currentPage++;
    displayMapsFromSupplier(() -> mapService.getOwnedMaps(currentPlayer.getId(), LOAD_PER_PAGE, currentPage));
  }

  private void replaceSearchResult(List<MapBean> maps, Pane pane) {
    List<MapCardController> controllers = maps.parallelStream()
        .map(map -> {
          MapCardController controller = uiService.loadFxml("theme/vault/map/map_card.fxml");
          controller.setMap(map);
          controller.setOnOpenDetailListener(this::onShowMapDetail);
          return controller;
        }).collect(Collectors.toList());

    Platform.runLater(() -> {
      pane.getChildren().clear();
    });
    // this needs to run in background thread so that ui thread can be updated in batches
    JavaFxUtil.assertBackgroundThread();
    Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining(mapCardControllers -> Platform.runLater(() -> {
      for (MapCardController mapCardController : mapCardControllers) {
        pane.getChildren().add(mapCardController.getRoot());
      }
      addMoreButton(pane);
    }));
  }

  private void addMoreButton(Pane pane) {
    switch (pane.getId()) {
      case "recommendedPane":
        pane.getChildren().add(moreRecommendedButton);
        break;
      case "mostLikedPane":
        pane.getChildren().add(moreMostLikedButton);
        break;
      case "newestPane":
        pane.getChildren().add(moreNewestButton);
        break;
      case "mostPlayedPane":
        pane.getChildren().add(moreMostPlayedButton);
        break;
      case "ladderPane":
        pane.getChildren().add(moreLadderButton);
        break;
      case "ownedPane":
        pane.getChildren().add(moreOwnedButton);
        break;
      default:
        // Do nothing
    }
  }

  private void displayMapsFromSupplier(Supplier<CompletableFuture<List<MapBean>>> mapsSupplier) {
    currentPage = 1;
    this.currentSupplier = mapsSupplier;
    mapsSupplier.get()
        .thenAccept(this::displayMaps)
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"),
              i18n.get("vault.maps.searchError"),
              throwable,
              i18n,
              reportingService
          ));
          enterShowroomState();
          return null;
        });
  }

  private void displayMaps(List<MapBean> maps) {
    replaceSearchResult(maps, searchResultPane);
    enterSearchResultState();
  }

  private void loadCurrentSupplier() {
    enterLoadingState();

    currentSupplier.get()
        .thenAccept(maps -> {
          replaceSearchResult(maps, searchResultPane);
          enterSearchResultState();
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"), i18n.get("vault.maps.searchError"),
              throwable, i18n, reportingService
          ));
          enterShowroomState();
          return null;
        });
  }

  public void onLoadPreviousButtonClicked() {
    currentPage--;
    loadCurrentSupplier();
  }

  public void onLoadNextButtonClicked() {
    currentPage++;
    loadCurrentSupplier();
  }

  private enum State {
    UNINITIALIZED, LOADING, SHOWROOM, SEARCH_RESULT
  }
}
