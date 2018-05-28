package com.faforever.client.map;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO I'd like to avoid the additional "getMost*" methods and always use the map query function instead, however,
// this is currently not viable since Elide can't yet sort by relationship attributes. Once it supports that
// (see https://github.com/yahoo/elide/issues/353), this can be refactored.
public class MapVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int LOAD_MORE_COUNT = 100;
  private static final int MAX_SEARCH_RESULTS = 100;
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
  public Pane searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public Node loadingLabel;
  public Pane newestPane;
  public Pane mostPlayedPane;
  public Pane mostLikedPane;
  public Pane mapVaultRoot;
  public ScrollPane scrollPane;
  public Button backButton;
  public SearchController searchController;
  public Button moreButton;
  public FlowPane ladderPane;
  private MapDetailController mapDetailController;
  private String query;
  private int currentPage;
  private Supplier<CompletableFuture<List<MapBean>>> currentSupplier;
  private SortConfig sortConfig;

  @Inject
  public MapVaultController(MapService mapService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                            UiService uiService, NotificationService notificationService) {
    this.mapService = mapService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.notificationService = notificationService;

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
    moreButton.managedProperty().bind(moreButton.visibleProperty());

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
    searchController.setSearchableProperties(SearchableProperties.MAP_PROPERTIES);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().mapSortConfigProperty());
  }

  private void searchByQuery(SearchConfig searchConfig) {
    this.query = searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"";
    this.sortConfig = searchConfig.getSortConfig();
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.findByQuery(query, ++currentPage, MAX_SEARCH_RESULTS, sortConfig));
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowLadderMapsEvent) {
      showMoreLadderdMaps();
    } else if (state.get() == State.UNINITIALIZED) {
      displayShowroomMaps();
    }
  }

  private void displayShowroomMaps() {
    enterLoadingState();
    mapService.getMostPlayedMaps(TOP_ELEMENT_COUNT, 1).thenAccept(maps -> replaceSearchResult(maps, mostPlayedPane))
        .thenCompose(aVoid -> mapService.getHighestRatedMaps(TOP_ELEMENT_COUNT, 1)).thenAccept(maps -> replaceSearchResult(maps, mostLikedPane))
        .thenCompose(aVoid -> mapService.getNewestMaps(TOP_ELEMENT_COUNT, 1)).thenAccept(maps -> replaceSearchResult(maps, newestPane))
        .thenCompose(aVoid -> mapService.getLadderMaps(TOP_ELEMENT_COUNT, 1).thenAccept(maps -> replaceSearchResult(maps, ladderPane)))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate maps", throwable);
          return null;
        });
  }

  private void replaceSearchResult(List<MapBean> maps, Pane pane) {
    Platform.runLater(() -> pane.getChildren().clear());
    appendSearchResult(maps, pane);
  }

  private void enterLoadingState() {
    state.set(State.LOADING);
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingLabel.setVisible(true);
    backButton.setVisible(true);
    moreButton.setVisible(false);
  }

  private void enterSearchResultState() {
    state.set(State.SEARCH_RESULT);
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingLabel.setVisible(false);
    backButton.setVisible(true);
    moreButton.setVisible(searchResultPane.getChildren().size() % MAX_SEARCH_RESULTS == 0);
  }

  private void enterShowroomState() {
    state.set(State.SHOWROOM);
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingLabel.setVisible(false);
    backButton.setVisible(false);
    moreButton.setVisible(false);
  }

  private void onShowMapDetail(MapBean map) {
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

    Stage mapUploadWindow = new Stage(StageStyle.TRANSPARENT);
    mapUploadWindow.initModality(Modality.NONE);
    mapUploadWindow.initOwner(getRoot().getScene().getWindow());

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(mapUploadWindow, mapUploadController.getRoot(), true, CLOSE);

    mapUploadWindow.show();
  }

  public void onRefreshButtonClicked() {
    mapService.evictCache();
    switch (state.get()) {
      case SHOWROOM:
        displayShowroomMaps();
        break;
      case SEARCH_RESULT:
        displayMapsFromSupplier(() -> mapService.findByQuery(query, 1, MAX_SEARCH_RESULTS, sortConfig));
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

  public void showMoreHighestRatedMaps() {
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.getHighestRatedMaps(LOAD_MORE_COUNT, ++currentPage));
  }

  public void showMoreMostRecentMaps() {
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.getNewestMaps(LOAD_MORE_COUNT, ++currentPage));
  }

  public void showMoreMostPlayedMaps() {
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.getMostPlayedMaps(LOAD_MORE_COUNT, ++currentPage));
  }

  public void showMoreLadderdMaps() {
    enterLoadingState();
    displayMapsFromSupplier(() -> mapService.getLadderMaps(LOAD_MORE_COUNT, ++currentPage));
  }

  private void appendSearchResult(List<MapBean> maps, Pane pane) {
    JavaFxUtil.assertBackgroundThread();

    ObservableList<Node> children = pane.getChildren();
    List<MapCardController> controllers = maps.parallelStream()
        .map(map -> {
          MapCardController controller = uiService.loadFxml("theme/vault/map/map_card.fxml");
          controller.setMap(map);
          controller.setOnOpenDetailListener(this::onShowMapDetail);
          return controller;
        }).collect(Collectors.toList());

    Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining(mapCardControllers -> Platform.runLater(() -> {
      for (MapCardController mapCardController : mapCardControllers) {
        children.add(mapCardController.getRoot());
      }
    }));
  }

  private void displayMapsFromSupplier(Supplier<CompletableFuture<List<MapBean>>> mapsSupplier) {
    currentPage = 0;
    this.currentSupplier = mapsSupplier;
    mapsSupplier.get()
        .thenAccept(this::displayMaps)
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.maps.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          enterShowroomState();
          return null;
        });
  }

  private void displayMaps(List<MapBean> maps) {
    Platform.runLater(() -> searchResultPane.getChildren().clear());
    appendSearchResult(maps, searchResultPane);
    Platform.runLater(this::enterSearchResultState);
  }

  public void onLoadMoreButtonClicked() {
    moreButton.setVisible(false);
    loadingLabel.setVisible(true);

    currentSupplier.get()
        .thenAccept(maps -> {
          appendSearchResult(maps, searchResultPane);
          enterSearchResultState();
        })
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.maps.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          enterShowroomState();
          return null;
        });
  }

  private enum State {
    UNINITIALIZED, LOADING, SHOWROOM, SEARCH_RESULT
  }
}
