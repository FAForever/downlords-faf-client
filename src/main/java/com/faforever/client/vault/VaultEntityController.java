package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.google.common.collect.Iterators;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public abstract class VaultEntityController<T> extends AbstractViewController<Node> {

  /**
   * How many cards should be badged into one UI thread runnable.
   */
  private static final int BATCH_SIZE = 10;
  protected static final int TOP_ELEMENT_COUNT = 7;
  protected final UiService uiService;
  protected final NotificationService notificationService;
  protected final I18n i18n;
  protected final PreferencesService preferencesService;
  protected final ReportingService reportingService;
  public Pane root;
  public StackPane vaultRoot;
  public VBox searchResultGroup;
  public Pane searchResultPane;
  public VBox showRoomGroup;
  public VBox loadingPane;
  public VBox contentPane;
  public Button backButton;
  public Button refreshButton;
  public Button uploadButton;
  public HBox paginationGroup;
  public ScrollPane scrollPane;
  public SearchController searchController;
  public Pagination pagination;
  public Button lastPageButton;
  public Button manageModsButton;
  public Button firstPageButton;
  protected SearchType searchType;
  protected ObjectProperty<State> state;
  protected CompletableFuture<Tuple<List<T>, Integer>> currentSupplier;
  protected int pageSize;
  public ComboBox<Integer> perPageComboBox;

  public VaultEntityController(UiService uiService, NotificationService notificationService, I18n i18n, PreferencesService preferencesService, ReportingService reportingService) {
    this.uiService = uiService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.reportingService = reportingService;

    state = new SimpleObjectProperty<>(State.UNINITIALIZED);
  }

  protected abstract Node getEntityCard(T t);

  protected abstract List<ShowRoomCategory> getShowRoomCategories();

  protected abstract void setSupplier(SearchConfig searchConfig, int page);

  protected abstract void onUploadButtonClicked();

  protected abstract Node getDetailView();

  protected abstract void onDisplayDetails(T t);

  protected abstract Class<? extends NavigateEvent> getDefaultNavigateEvent();

  protected abstract void handleSpecialNavigateEvent(NavigateEvent navigateEvent);

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    refreshButton.managedProperty().bind(refreshButton.visibleProperty());
    pagination.managedProperty().bind(pagination.visibleProperty());

    firstPageButton.managedProperty().bind(firstPageButton.visibleProperty());
    firstPageButton.disableProperty().bind(pagination.currentPageIndexProperty().isEqualTo(0));
    lastPageButton.managedProperty().bind(lastPageButton.visibleProperty());
    lastPageButton.disableProperty().bind(pagination.currentPageIndexProperty()
        .isEqualTo(pagination.pageCountProperty().subtract(1)));

    showRoomGroup.managedProperty().bind(showRoomGroup.visibleProperty());

    manageModsButton.managedProperty().bind(manageModsButton.visibleProperty());

    backButton.setOnAction((event -> onBackButtonClicked()));
    refreshButton.setOnAction((event -> onRefreshButtonClicked()));
    uploadButton.setOnAction((event -> onUploadButtonClicked()));

    searchController.setSearchListener(this::onSearch);
    perPageComboBox.getItems().addAll(5, 10, 20, 50, 100, 200);
    perPageComboBox.setValue(20);
    perPageComboBox.getEditor().setOnAction(event -> changePerPageCount());
    perPageComboBox.setOnAction((event -> changePerPageCount()));
    pageSize = perPageComboBox.getValue();

    BooleanBinding inSearchableState = Bindings.createBooleanBinding(() -> state.get() != State.SEARCHING, state);
    searchController.setSearchButtonDisabledCondition(inSearchableState);

    pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
          if (!oldValue.equals(newValue)) {
            SearchConfig searchConfig = searchController.getLastSearchConfig();
            onPageChange(searchConfig, newValue.intValue() + 1, false);
            pagination.setMaxPageIndicatorCount(10);
          }
        }
    );
    paginationGroup.managedProperty().bind(paginationGroup.visibleProperty());
    firstPageButton.setOnAction(event -> pagination.setCurrentPageIndex(0));
    lastPageButton.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getPageCount() - 1));

    Node detailView = getDetailView();

    detailView.setVisible(false);
    detailView.requestFocus();

    vaultRoot.getChildren().add(detailView);
    AnchorPane.setTopAnchor(detailView, 0d);
    AnchorPane.setRightAnchor(detailView, 0d);
    AnchorPane.setBottomAnchor(detailView, 0d);
    AnchorPane.setLeftAnchor(detailView, 0d);
  }

  protected void loadShowRoom() {
    JavaFxUtil.assertApplicationThread();
    enterSearchingState();
    showRoomGroup.getChildren().clear();
    Object monitorForAddingFutures = new Object();
    List<ShowRoomCategory> showRoomCategories = getShowRoomCategories();
    AtomicReference<CompletableFuture<?>> loadingReplaysFutureReference = new AtomicReference<>(CompletableFuture.completedFuture(null));

    List<VBox> childrenToAdd = showRoomCategories.parallelStream()
        .map(showRoomCategory -> {
          VaultEntityShowRoomController vaultEntityShowRoomController = loadShowRoom(showRoomCategory);
          synchronized (monitorForAddingFutures) {
            loadingReplaysFutureReference.set(loadingReplaysFutureReference.get().thenCompose(ignored -> showRoomCategory.getEntitySupplier().get())
                .thenAccept(result -> {
                  if (result.getFirst().isEmpty()) {
                    vaultEntityShowRoomController.getRoot().setVisible(false);
                  }
                  populate(result.getFirst(), vaultEntityShowRoomController.getPane());
                }));
          }
          return vaultEntityShowRoomController.getRoot();
        })
        .collect(Collectors.toList());

    loadingReplaysFutureReference.get()
        .thenRun(() -> Platform.runLater(() -> {
          showRoomGroup.getChildren().addAll(childrenToAdd);
          enterShowRoomState();
        }))
        .exceptionally(throwable -> {
          log.warn("Could not populate replays", throwable);
          return null;
        });
  }

  @NotNull
  private VaultEntityShowRoomController loadShowRoom(ShowRoomCategory showRoomCategory) {
    VaultEntityShowRoomController vaultEntityShowRoomController = uiService.loadFxml("theme/vault/vault_entity_show_room.fxml");
    vaultEntityShowRoomController.getLabel().setText(i18n.get(showRoomCategory.getI18nKey()));
    vaultEntityShowRoomController.getMoreButton().setOnAction(event -> {
      searchType = showRoomCategory.getSearchType();
      onFirstPageOpened(null);
    });
    return vaultEntityShowRoomController;
  }

  protected void changePerPageCount() {
    pageSize = perPageComboBox.getValue();
    if (state.get() == State.RESULT) {
      SearchConfig searchConfig = searchController.getLastSearchConfig();
      onPageChange(searchConfig, pagination.getCurrentPageIndex() + 1, true);
    }
  }

  protected void enterSearchingState() {
    state.set(State.SEARCHING);

    showRoomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
    backButton.setVisible(false);
    paginationGroup.setVisible(false);
  }

  protected void enterResultState() {
    state.set(State.RESULT);

    showRoomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
    backButton.setVisible(true);
    paginationGroup.setVisible(true);
  }

  protected void enterShowRoomState() {
    state.set(State.SHOWROOM);

    showRoomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
    backButton.setVisible(false);
    paginationGroup.setVisible(false);
  }

  protected void onPageChange(SearchConfig searchConfig, int page, boolean firstLoad) {
    enterSearchingState();
    setSupplier(searchConfig, page);
    displayFromSupplier(() -> currentSupplier, firstLoad);
  }

  protected void displaySearchResult(List<T> results) {
    JavaFxUtil.assertBackgroundThread();
    enterSearchingState();
    populate(results, searchResultPane);
    Platform.runLater(this::enterResultState);
  }

  protected void displayFromSupplier(Supplier<CompletableFuture<Tuple<List<T>, Integer>>> supplier, boolean firstLoad) {
    supplier.get()
        .thenAccept(tuple -> {
          displaySearchResult(tuple.getFirst());
          if (firstLoad) {
            //when theres no search results the page count should be 1, 0 (which is returned) results in infinite pages
            Platform.runLater(() -> pagination.setPageCount(Math.max(1, tuple.getSecond())));
          }
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateErrorNotification(
              i18n.get("errorTitle"), i18n.get("vault.searchError"), throwable, i18n, reportingService
          ));
          enterResultState();
          return null;
        });
  }

  protected void populate(List<T> results, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    List<Node> childrenToAdd = results.parallelStream()
        .map(this::getEntityCard)
        .collect(Collectors.toList());

    Platform.runLater(children::clear);

    Iterators.partition(childrenToAdd.iterator(), BATCH_SIZE).forEachRemaining(newChildren -> Platform.runLater(() -> {
      children.addAll(newChildren);
      // The more button is referenced by the panes user data
      Object userData = pane.getUserData();
      if (userData == null) {
        return;
      }
      pane.getChildren().add((Node) userData);
    }));
  }

  protected void onFirstPageOpened(SearchConfig searchConfig) {
    onPageChange(searchConfig, 1, true);
    if (pagination.getCurrentPageIndex() != 0) {
      pagination.setCurrentPageIndex(0);
    }
  }

  protected void onSearch(SearchConfig searchConfig) {
    searchType = SearchType.SEARCH;
    onFirstPageOpened(searchConfig);
  }

  protected void onRefreshButtonClicked() {
    if (state.get() == State.RESULT) {
      onPageChange(searchController.getLastSearchConfig(), pagination.currentPageIndexProperty().getValue() + 1, false);
    } else {
      loadShowRoom();
    }
  }

  protected void onBackButtonClicked() {
    loadShowRoom();
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    Class<? extends NavigateEvent> defaultNavigateEvent = getDefaultNavigateEvent();
    if (!(navigateEvent.getClass().equals(defaultNavigateEvent))) {
      if (state.get() == State.UNINITIALIZED) {
        state.addListener(new ChangeListener<>() {
          @Override
          public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            if (newValue != State.UNINITIALIZED) {
              state.removeListener(this);
              handleSpecialNavigateEvent(navigateEvent);
            }
          }
        });
        loadShowRoom();
      } else {
        handleSpecialNavigateEvent(navigateEvent);
      }
    } else if (state.get() == State.UNINITIALIZED) {
      loadShowRoom();
    }
  }

  @Override
  public Node getRoot() {
    return root;
  }

  protected enum State {
    SEARCHING, RESULT, UNINITIALIZED, SHOWROOM
  }

  protected enum SearchType {
    SEARCH, OWN, NEWEST, HIGHEST_RATED, PLAYER, RECOMMENDED, LADDER, PLAYED, HIGHEST_RATED_UI
  }

  @Value
  public class ShowRoomCategory {
    Supplier<CompletableFuture<Tuple<List<T>, Integer>>> entitySupplier;
    SearchType searchType;
    String i18nKey;
  }
}
