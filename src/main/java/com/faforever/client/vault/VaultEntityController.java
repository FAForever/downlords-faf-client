package com.faforever.client.vault;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.commons.api.dto.ApiException;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public abstract class VaultEntityController<T> extends NodeController<Node> {

  public static final int TOP_ELEMENT_COUNT = 7;

  protected final UiService uiService;
  protected final NotificationService notificationService;
  protected final I18n i18n;
  protected final ReportingService reportingService;
  protected final VaultPrefs vaultPrefs;
  protected final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  protected final ObjectProperty<State> state = new SimpleObjectProperty<>(State.UNINITIALIZED);

  private final Map<ShowRoomCategory, ObservableList<T>> showRoomEntities = new HashMap<>();
  private final List<VBox> showRoomRoots = new ArrayList<>();
  private final ObservableList<T> resultEntities = FXCollections.observableArrayList();
  private final ObservableList<Node> resultCardRoots = FXCollections.observableArrayList();

  public Pane root;
  public StackPane vaultRoot;
  public HBox searchBox;
  public VBox searchResultGroup;
  public Pane searchResultPane;
  public Separator searchSeparator;
  public VBox showRoomGroup;
  public VBox loadingPane;
  public Button backButton;
  public Button refreshButton;
  public Button uploadButton;
  public HBox paginationGroup;
  public ScrollPane scrollPane;
  public SearchController searchController;
  public Pagination pagination;
  public Button lastPageButton;
  public Button manageVaultButton;
  public Button firstPageButton;
  public SearchType searchType;
  public int pageSize;
  public ComboBox<Integer> perPageComboBox;

  protected CompletableFuture<Tuple2<List<T>, Integer>> currentSupplier;
  private final CompletableFuture<Void> showRoomInitializedFuture = CompletableFuture.runAsync(this::initializeShowRoomCards);

  protected abstract void initSearchController();

  protected abstract VaultEntityCardController<T> createEntityCard();

  protected abstract List<ShowRoomCategory> getShowRoomCategories();

  protected abstract void setSupplier(SearchConfig searchConfig);

  protected abstract void onUploadButtonClicked();

  protected abstract void onManageVaultButtonClicked();

  protected abstract Node getDetailView();

  protected abstract void onDisplayDetails(T t);

  protected abstract Class<? extends NavigateEvent> getDefaultNavigateEvent();

  protected abstract void handleSpecialNavigateEvent(NavigateEvent navigateEvent);

  @Override
  protected void onInitialize() {
    super.onInitialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);
    JavaFxUtil.bindManagedToVisible(loadingPane, searchResultGroup, backButton, refreshButton, pagination, firstPageButton, lastPageButton, showRoomGroup, searchBox, searchSeparator);

    firstPageButton.disableProperty().bind(pagination.currentPageIndexProperty().isEqualTo(0));
    lastPageButton.disableProperty()
        .bind(pagination.currentPageIndexProperty().isEqualTo(pagination.pageCountProperty().subtract(1)));

    backButton.setOnAction(event -> onBackButtonClicked());
    refreshButton.setOnAction(event -> onRefreshButtonClicked());
    uploadButton.setOnAction(event -> onUploadButtonClicked());
    manageVaultButton.setOnAction(event -> onManageVaultButtonClicked());

    searchController.setSearchListener(this::onSearch);
    perPageComboBox.getItems().addAll(5, 10, 20, 50, 100);
    perPageComboBox.valueProperty().addListener(((observable, oldValue, newValue) -> onPerPageCountChanged(oldValue == null ? 0 : oldValue, newValue)));
    perPageComboBox.setValue(20);
    perPageComboBox.setOnAction((event -> changePerPageCount()));
    pageSize = perPageComboBox.getValue();

    initSearchController();

    searchController.setSearchButtonDisabledCondition(state.map(state1 -> state1 == State.SEARCHING));

    pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
      if (!oldValue.equals(newValue)) {
        SearchConfig searchConfig = searchController.getLastSearchConfig();
        onPageChange(searchConfig, false);
        pagination.setMaxPageIndicatorCount(10);
      }
    });
    paginationGroup.managedProperty().bind(paginationGroup.visibleProperty());
    firstPageButton.setOnAction(event -> pagination.setCurrentPageIndex(0));
    lastPageButton.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getPageCount() - 1));

    Node detailView = getDetailView();

    detailView.setVisible(false);
    detailView.requestFocus();

    vaultRoot.getChildren().add(detailView);
    JavaFxUtil.setAnchors(detailView, 0d);

    showRoomGroup.visibleProperty().bind(state.isEqualTo(State.SHOWROOM));
    searchResultGroup.visibleProperty().bind(state.isEqualTo(State.RESULT));
    backButton.visibleProperty().bind(state.isEqualTo(State.RESULT));
    paginationGroup.visibleProperty().bind(state.isEqualTo(State.RESULT));
    loadingPane.visibleProperty().bind(state.isEqualTo(State.SEARCHING));

    Bindings.bindContent(searchResultPane.getChildren(), resultCardRoots);
  }

  @Override
  public void onShow() {
    if (state.get() == State.UNINITIALIZED) {
      loadShowRooms();
    }
  }

  private void initializeShowRoomCards() {
    List<ShowRoomCategory> showRoomCategories = getShowRoomCategories();
    showRoomCategories.forEach(showRoomCategory -> {
      ObservableList<Node> categoryRoots = FXCollections.observableArrayList();
      ObservableList<T> categoryEntities = FXCollections.observableArrayList();

      IntStream.range(0, TOP_ELEMENT_COUNT).mapToObj(i -> {
        VaultEntityCardController<T> entityCard = createEntityCard();
        entityCard.entityProperty().bind(Bindings.valueAt(categoryEntities, i));
        Node entityCardRoot = entityCard.getRoot();
        entityCardRoot.visibleProperty().bind(entityCard.entityProperty().isNotNull());
        JavaFxUtil.bindManagedToVisible(entityCardRoot);
        return entityCardRoot;
      }).forEach(categoryRoots::add);
      showRoomEntities.put(showRoomCategory, categoryEntities);

      VaultEntityShowRoomController showRoomController = loadShowRoom(showRoomCategory);
      JavaFxUtil.bindManagedToVisible(showRoomController.getRoot());
      showRoomController.getRoot().visibleProperty().bind(Bindings.isNotEmpty(categoryEntities));
      showRoomController.setChildren(categoryRoots);
      showRoomRoots.add(showRoomController.getRoot());
    });
    fxApplicationThreadExecutor.execute(() -> showRoomGroup.getChildren().setAll(showRoomRoots));
  }

  private void onPerPageCountChanged(Integer oldValue, Integer newValue) {
    if (newValue < oldValue) {
      fxApplicationThreadExecutor.execute(() -> resultCardRoots.remove(newValue, oldValue));
    } else if (newValue > oldValue) {
      CompletableFuture.runAsync(() -> {
        List<Node> newNodes = IntStream.range(oldValue, newValue).mapToObj(i -> {
          VaultEntityCardController<T> entityCard = createEntityCard();
          entityCard.entityProperty().bind(Bindings.valueAt(resultEntities, i));
          Node entityCardRoot = entityCard.getRoot();
          entityCardRoot.visibleProperty().bind(entityCard.entityProperty().isNotNull());
          JavaFxUtil.bindManagedToVisible(entityCardRoot);
          return entityCardRoot;
        }).toList();

        fxApplicationThreadExecutor.execute(() -> resultCardRoots.addAll(newNodes));
      });
    }

    pageSize = newValue;
    if (state.get() == State.RESULT) {
      SearchConfig searchConfig = searchController.getLastSearchConfig();
      onPageChange(searchConfig, true);
    }
  }

  protected void enterSearchingState() {
    fxApplicationThreadExecutor.execute(() -> state.set(State.SEARCHING));
  }

  protected void enterResultState() {
    fxApplicationThreadExecutor.execute(() -> state.set(State.RESULT));
  }

  protected void enterShowRoomState() {
    fxApplicationThreadExecutor.execute(() -> state.set(State.SHOWROOM));
  }

  protected void loadShowRooms() {
    enterSearchingState();
    showRoomInitializedFuture.thenComposeAsync(aVoid -> CompletableFuture.allOf(showRoomEntities.entrySet()
        .stream()
        .map(entry -> entry.getKey()
            .getEntitySupplier()
            .get()
            .thenAcceptAsync(results -> entry.getValue().setAll(results.getT1()), fxApplicationThreadExecutor))
        .toArray(CompletableFuture[]::new))).thenRunAsync(this::enterShowRoomState);
  }

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
      onPageChange(searchConfig, true);
    }
  }

  protected void onPageChange(SearchConfig searchConfig, boolean firstLoad) {
    enterSearchingState();
    setSupplier(searchConfig);
    displayFromSupplier(() -> currentSupplier, firstLoad);
  }

  protected void displaySearchResult(List<T> results) {
    fxApplicationThreadExecutor.execute(() -> {
      resultEntities.setAll(results);
      enterResultState();
    });
  }

  protected void displayFromSupplier(Supplier<CompletableFuture<Tuple2<List<T>, Integer>>> supplier,
                                     boolean firstLoad) {
    supplier.get().thenAcceptAsync(tuple -> {
      displaySearchResult(tuple.getT1());
      if (firstLoad) {
        //when theres no search results the page count should be 1, 0 (which is returned) results in infinite pages
        fxApplicationThreadExecutor.execute(() -> pagination.setPageCount(Math.max(1, tuple.getT2())));
      }
    }).exceptionally(throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      if (throwable instanceof ApiException) {
        String query = searchController.queryTextField.getText();
        log.warn("Bad search parameter in query {}", query, throwable);
        notificationService.addImmediateWarnNotification("vault.badSearch", throwable.getLocalizedMessage(), query);
      } else {
        log.error("Vault search error", throwable);
        notificationService.addImmediateErrorNotification(throwable, "vault.searchError");
      }
      enterShowRoomState();
      return null;
    });
  }

  protected void onFirstPageOpened(SearchConfig searchConfig) {
    if (pagination.getCurrentPageIndex() != 0) {
      pagination.setCurrentPageIndex(0);
    }
    onPageChange(searchConfig, true);
  }

  protected void onSearch(SearchConfig searchConfig) {
    searchType = SearchType.SEARCH;
    onFirstPageOpened(searchConfig);
  }

  protected void onRefreshButtonClicked() {
    if (state.get() == State.RESULT) {
      onPageChange(searchController.getLastSearchConfig(), false);
    } else {
      loadShowRooms();
    }
  }

  protected void onBackButtonClicked() {
    loadShowRooms();
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    Class<? extends NavigateEvent> defaultNavigateEvent = getDefaultNavigateEvent();
    if (!(navigateEvent.getClass().equals(defaultNavigateEvent)) && !navigateEvent.getClass()
        .equals(NavigateEvent.class)) {
      handleSpecialNavigateEvent(navigateEvent);
    } else if (state.get() == State.UNINITIALIZED) {
      loadShowRooms();
    }
  }

  @Override
  public Node getRoot() {
    return root;
  }

  protected enum State {
    SEARCHING, RESULT, UNINITIALIZED, SHOWROOM
  }

  public enum SearchType {
    SEARCH, OWN, NEWEST, HIGHEST_RATED, PLAYER, RECOMMENDED, MAP_POOL, PLAYED, HIGHEST_RATED_UI
  }

  @Value
  public class ShowRoomCategory {
    Supplier<CompletableFuture<Tuple2<List<T>, Integer>>> entitySupplier;
    SearchType searchType;
    String i18nKey;
  }
}
