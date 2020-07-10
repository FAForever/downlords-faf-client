package com.faforever.client.leaderboard;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenLadder1v1LeaderboardEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Window;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardControllerTest extends AbstractPlainJavaFxTest {

  private LeaderboardController instance;

  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private LeaderboardUserContextMenuController userContextMenuController;

  @Before
  public void setUp() throws Exception {
    instance = new LeaderboardController(leaderboardService, notificationService, i18n, reportingService, uiService);

    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() throws Exception {
    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        new LeaderboardEntry(), new LeaderboardEntry()
    )));

    CountDownLatch loadedLatch = new CountDownLatch(1);
    instance.ratingTable.itemsProperty().addListener(observable -> loadedLatch.countDown());
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);

    instance.display(new OpenLadder1v1LeaderboardEvent());

    assertTrue(loadedLatch.await(3, TimeUnit.SECONDS));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {
    LeaderboardEntry entry1 = new LeaderboardEntry();
    entry1.setUsername("Aa");
    LeaderboardEntry entry2 = new LeaderboardEntry();
    entry2.setUsername("Ab");

    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);
    instance.display(new OpenLadder1v1LeaderboardEvent());

    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), nullValue());

    instance.searchTextField.setText("aa");
    assertThat(instance.ratingTable.getItems(), hasSize(2));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem().getUsername(), is("Aa"));
  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {
    LeaderboardEntry entry1 = new LeaderboardEntry();
    entry1.setUsername("Aa");
    LeaderboardEntry entry2 = new LeaderboardEntry();
    entry2.setUsername("Ab");

    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);
    instance.display(new OpenLadder1v1LeaderboardEvent());

    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), nullValue());

    instance.searchTextField.setText("b");
    assertThat(instance.ratingTable.getItems(), hasSize(2));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem().getUsername(), is("Ab"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.leaderboardRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnContextMenuRequested() {
    ContextMenu contextMenu = mock(ContextMenu.class);
    ContextMenuEvent contextMenuEvent = mock(ContextMenuEvent.class);
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();

    when(userContextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme\\leaderboard\\leaderboard_user_context_menu.fxml")).thenReturn(userContextMenuController);

    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.leaderboardRoot));

    Player player = PlayerBuilder.create("mockito").defaultValues().get();
    userContextMenuController.setPlayer(player);
    //when(instance.selectedEntryList.get(anyInt())).thenReturn(leaderboardEntry);
    when(leaderboardService.getPlayerObjectsById(any(String.class))).thenReturn(CompletableFuture.completedFuture(Arrays.asList(player)));

    WaitForAsyncUtils.waitForFxEvents();

    instance.onContextMenuRequested(contextMenuEvent);

    verify(contextMenu).show(any(Window.class), anyDouble(), anyDouble());
  }
}
