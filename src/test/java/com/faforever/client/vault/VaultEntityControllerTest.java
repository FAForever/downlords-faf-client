package com.faforever.client.vault;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.vault.VaultEntityController.TOP_ELEMENT_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class VaultEntityControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private SearchController searchController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private VaultEntityShowRoomController vaultEntityShowRoomController;

  private VaultEntityController<Integer> instance;
  private List<Integer> items;
  private VBox showRoomRoot;
  private Label showRoomLabel;
  private Button moreButton;
  private FlowPane showRoomPane;


  private List<Integer> createMockElements(int numberOfElements) {
    List<Integer> elements = new ArrayList<>(numberOfElements);
    for (int i = 0; i < numberOfElements; i++) {
      elements.add(i);
    }
    return elements;
  }

  private List<Integer> getMockPageElements(List<Integer> elements, int pageSize, int page) {
    return elements.subList(Math.min((page) * pageSize, elements.size()),
        Math.min((page + 1) * pageSize, elements.size()));
  }

  private CompletableFuture<Tuple<List<Integer>, Integer>> asFuture(Tuple<List<Integer>, Integer> page) {
    return CompletableFuture.completedFuture(page);
  }

  private CompletableFuture<Tuple<List<Integer>, Integer>> mocksAsFuture(List<Integer> elements, int pageSize, int page) {
    return CompletableFuture.completedFuture(new Tuple<>(getMockPageElements(elements, pageSize, page),
        (int) Math.ceil((double) elements.size() / pageSize)));
  }

  @Before
  public void setUp() throws Exception {
    showRoomRoot = new VBox();
    showRoomLabel = new Label();
    showRoomPane = new FlowPane();
    moreButton = new Button();
    showRoomPane.setUserData(moreButton);

    when(i18n.get("mock")).thenReturn("mock");
    when(uiService.loadFxml("theme/vault/vault_entity_show_room.fxml")).thenReturn(vaultEntityShowRoomController);
    when(vaultEntityShowRoomController.getRoot()).thenReturn(showRoomRoot);
    when(vaultEntityShowRoomController.getLabel()).thenReturn(showRoomLabel);
    when(vaultEntityShowRoomController.getMoreButton()).thenReturn(moreButton);
    when(vaultEntityShowRoomController.getPane()).thenReturn(showRoomPane);

    items = createMockElements(50);
    instance = new VaultEntityController<>(uiService, notificationService, i18n, preferencesService, reportingService) {
      @Override
      protected void initSearchController() {
        //Do Nothing
      }

      @Override
      protected Node getEntityCard(Integer integer) {
        GridPane card = new GridPane();
        card.setUserData(integer);
        return card;
      }

      @Override
      protected List<ShowRoomCategory> getShowRoomCategories() {
        List<ShowRoomCategory> categories = new ArrayList<>();
        categories.add(new ShowRoomCategory(() -> mocksAsFuture(items, TOP_ELEMENT_COUNT, 0), SearchType.NEWEST, "mock"));
        return categories;
      }

      @Override
      protected void setSupplier(SearchConfig searchConfig) {
        currentSupplier = mocksAsFuture(items, pageSize, pagination.getCurrentPageIndex());
      }

      @Override
      protected void onUploadButtonClicked() {
      }

      @Override
      protected Node getDetailView() {
        return new AnchorPane();
      }

      @Override
      protected void onDisplayDetails(Integer integer) {
      }

      @Override
      protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
        return NavigateEvent.class;
      }

      @Override
      protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
      }
    };

    loadFxml("theme/vault/vault_entity.fxml", clazz -> {
      if (clazz == SearchController.class) {
        return searchController;
      }
      if (clazz == LogicalNodeController.class) {
        return logicalNodeController;
      }
      if (clazz == SpecificationController.class) {
        return specificationController;
      }
      return instance;
    }, instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    assertEquals(instance.root, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testOnDisplay() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
    assertEquals(1, instance.showRoomGroup.getChildren().size());
    assertEquals(TOP_ELEMENT_COUNT + 1, showRoomPane.getChildren().size());
  }

  @Test
  public void testEmptyShowRoom() {
    items = createMockElements(0);
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(showRoomRoot.isVisible());
    assertEquals(1, instance.showRoomGroup.getChildren().size());
  }

  @Test
  public void testPagination() {
    List<Integer> elePage1 = getMockPageElements(items, instance.pageSize, 0);
    List<Integer> elePage3 = getMockPageElements(items, instance.pageSize, 2);

    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    // first page / search results
    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < elePage1.size(); i++) {
      assertEquals(elePage1.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }

    // third page
    instance.pagination.setCurrentPageIndex(2);
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(10, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < 10; i++) {
      assertEquals(elePage3.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }
  }

  @Test
  public void testLastPageButton() {
    List<Integer> elePage3 = getMockPageElements(items, instance.pageSize, 2);

    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    instance.lastPageButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(10, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < 10; i++) {
      assertEquals(elePage3.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }
  }

  @Test
  public void testFirstPageButton() {
    List<Integer> elePage1 = getMockPageElements(items, instance.pageSize, 0);

    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    instance.firstPageButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < 10; i++) {
      assertEquals(elePage1.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }
  }

  @Test
  public void testPageSize() {
    int newPageSize = 30;

    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    instance.perPageComboBox.setValue(newPageSize);
    WaitForAsyncUtils.waitForFxEvents();
    List<Integer> elePage1 = getMockPageElements(items, newPageSize, 0);

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < instance.pageSize; i++) {
      assertEquals(elePage1.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }
  }

  @Test
  public void testPageSizeChange() {
    int newPageSize = 30;

    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    List<Integer> elePage1 = getMockPageElements(items, instance.pageSize, 0);

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < elePage1.size(); i++) {
      assertEquals(elePage1.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }

    instance.perPageComboBox.setValue(newPageSize);
    WaitForAsyncUtils.waitForFxEvents();
    instance.changePerPageCount();
    WaitForAsyncUtils.waitForFxEvents();
    elePage1 = getMockPageElements(items, newPageSize, 0);

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(newPageSize, instance.searchResultPane.getChildren().size());
    for (int i = 0; i < newPageSize; i++) {
      assertEquals(elePage1.get(i), instance.searchResultPane.getChildren().get(i).getUserData());
    }
  }

  @Test
  public void testOnFirstPageChange() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());

    instance.pagination.setCurrentPageIndex(1);
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(1, instance.pagination.getCurrentPageIndex());

    instance.onFirstPageOpened(null);
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
  }

  @Test
  public void testOnBackButton() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());

    Platform.runLater(() -> instance.backButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
  }

  @Test
  public void testOnSearch() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    Platform.runLater(() -> instance.onSearch(null));
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }

  @Test
  public void testRefreshShowRoom() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    Platform.runLater(() -> instance.refreshButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
  }

  @Test
  public void testRefreshSearch() {
    Platform.runLater(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    Platform.runLater(() -> instance.onSearch(null));
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());

    Platform.runLater(() -> instance.refreshButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }
}

