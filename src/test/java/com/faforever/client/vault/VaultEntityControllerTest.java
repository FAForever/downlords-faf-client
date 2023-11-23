package com.faforever.client.vault;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Disabled("Flaky on github")
public class VaultEntityControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;

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
  @Spy
  private VaultPrefs vaultPrefs;

  private VaultEntityController<Integer> instance;
  private List<Integer> items;
  private VBox showRoomRoot;
  private Button moreButton;
  private FlowPane showRoomPane;


  private List<Integer> getMockPageElements(List<Integer> elements, int pageSize, int page) {
    return elements.subList(Math.min((page) * pageSize, elements.size()),
        Math.min((page + 1) * pageSize, elements.size()));
  }

  private CompletableFuture<Tuple2<List<Integer>, Integer>> mocksAsFuture(List<Integer> elements, int pageSize, int page) {
    return Mono.zip(Mono.just(getMockPageElements(elements, pageSize, page)),
        Mono.just((int) Math.ceil((double) elements.size() / pageSize))).toFuture();
  }

  @BeforeEach
  public void setUp() throws Exception {
    showRoomRoot = new VBox();
    Label showRoomLabel = new Label();
    showRoomPane = new FlowPane();
    moreButton = new Button();
    showRoomPane.setUserData(moreButton);

    when(i18n.get("mock")).thenReturn("mock");
    when(uiService.loadFxml("theme/vault/vault_entity_show_room.fxml")).thenReturn(vaultEntityShowRoomController);
    when(vaultEntityShowRoomController.getRoot()).thenReturn(showRoomRoot);
    when(vaultEntityShowRoomController.getLabel()).thenReturn(showRoomLabel);
    when(vaultEntityShowRoomController.getMoreButton()).thenReturn(moreButton);
    when(vaultEntityShowRoomController.getPane()).thenReturn(showRoomPane);

    items = IntStream.range(0, 50).boxed().toList();
    instance = new VaultEntityController<>(uiService, notificationService, i18n, reportingService, vaultPrefs, fxApplicationThreadExecutor) {
      @Override
      protected void initSearchController() {
        //Do Nothing
      }

      @Override
      protected VaultEntityCardController<Integer> createEntityCard() {
        return new VaultEntityCardController<>() {
          @Override
          public Node getRoot() {
            return new Pane();
          }
        };
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
      protected void onManageVaultButtonClicked() {

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
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    assertEquals(instance.root, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testOnDisplay() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
  }

  @Test
  @Disabled("Flaky on github")
  public void testEmptyShowRoom() {
    items = IntStream.range(0, 0).boxed().toList();
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    assertTrue(showRoomRoot.isVisible());
    assertEquals(1, instance.showRoomGroup.getChildren().size());
  }

  @Test
  public void testPagination() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    // first page / search results
    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());

    // third page
    instance.pagination.setCurrentPageIndex(2);
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(10, instance.searchResultPane.getChildren().stream().filter(Node::isVisible).count());
  }

  @Test
  @Disabled("Flaky on github")
  public void testLastPageButton() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    instance.lastPageButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(10, instance.searchResultPane.getChildren().stream().filter(Node::isVisible).count());
  }

  @Test
  public void testFirstPageButton() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    instance.firstPageButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
  }

  @Test
  public void testPageSize() {
    int newPageSize = 30;

    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    instance.perPageComboBox.setValue(newPageSize);
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());
  }

  @Test
  public void testPageSizeChange() {
    int newPageSize = 30;

    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(instance.pageSize, instance.searchResultPane.getChildren().size());

    instance.perPageComboBox.setValue(newPageSize);
    WaitForAsyncUtils.waitForFxEvents();
    instance.changePerPageCount();
    WaitForAsyncUtils.waitForFxEvents();

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(0, instance.pagination.getCurrentPageIndex());
    assertEquals(newPageSize, instance.searchResultPane.getChildren().size());
  }

  @Test
  public void testOnFirstPageChange() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
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
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    moreButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());

    runOnFxThreadAndWait(() -> instance.backButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
  }

  @Test
  @Disabled("Flaky Test")
  public void testOnSearch() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    runOnFxThreadAndWait(() -> instance.onSearch(null));
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }

  @Test
  public void testRefreshShowRoom() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    runOnFxThreadAndWait(() -> instance.refreshButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());
  }

  @Test
  public void testRefreshSearch() {
    runOnFxThreadAndWait(() -> instance.display(new NavigateEvent(NavigationItem.MAP)));
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.showRoomGroup.isVisible());
    assertFalse(instance.searchResultGroup.isVisible());

    runOnFxThreadAndWait(() -> instance.onSearch(null));
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());

    runOnFxThreadAndWait(() -> instance.refreshButton.fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.showRoomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }
}

