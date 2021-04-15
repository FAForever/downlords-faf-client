package com.faforever.client.teammatchmaking;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBeanBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingQueueItemControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PlayerService playerService;
  @Mock
  private MapService mapService;
  @Mock
  private I18n i18n;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private Player player;

  private MatchmakingQueueItemController instance;
  private MatchmakingQueue queue;

  @Before
  public void setUp() throws Exception {
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.get("teammatchmaking.playersInQueue", 1)).thenReturn("");
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    prepareParty();

    instance = new MatchmakingQueueItemController(playerService, mapService, teamMatchmakingService, i18n, eventBus);
    when(teamMatchmakingService.getPlayersInGame()).thenReturn(FXCollections.observableSet());
    loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml", clazz -> instance);
    queue = new MatchmakingQueue();
    queue.setQueueName("1");
    queue.setPlayersInQueue(1);
    queue.setTeamSize(2);
    queue.setJoined(false);
    instance.setQueue(queue);
  }

  private void prepareParty() {
    Party party = new Party();
    party.setOwner(player);
    PartyMember member = mock(PartyMember.class);
    ObservableList<PartyMember> members = FXCollections.observableArrayList();
    members.add(member);
    party.setMembers(members);
    when(teamMatchmakingService.getParty()).thenReturn(party);
  }

  @Test
  public void testShowMapPool() {
    instance.showMapPool(new ActionEvent());

    ArgumentCaptor<ShowMapPoolEvent> captor = ArgumentCaptor.forClass(ShowMapPoolEvent.class);
    verify(eventBus).post(captor.capture());
  }

  @Test
  public void testOnJoinLeaveQueueButtonClicked() throws MalformedURLException {
    MapBean map1 = MapBeanBuilder.create().defaultValues().folderName("map1").get();
    MapBean map2 = MapBeanBuilder.create().defaultValues().folderName("map2").get();
    MapBean map3 = MapBeanBuilder.create().defaultValues().folderName("map3").get();

    when(teamMatchmakingService.joinQueue(any())).thenReturn(true);
    when(mapService.getAllMatchmakerMaps(queue)).thenReturn(CompletableFuture.completedFuture(
        List.of(map1, map2, map3)));
    when(mapService.isInstalled(map1.getFolderName())).thenReturn(false);
    when(mapService.isInstalled(map2.getFolderName())).thenReturn(true);
    when(mapService.isInstalled(map3.getFolderName())).thenReturn(false);

    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).downloadAndInstallMap(eq(map1), isNull(), isNull());
    verify(mapService, never()).downloadAndInstallMap(eq(map2), isNull(), isNull());
    verify(mapService).downloadAndInstallMap(eq(map3), isNull(), isNull());
    verify(teamMatchmakingService).joinQueue(instance.queue);
    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(true));

    instance.queue.setJoined(true);
    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    verify(teamMatchmakingService).leaveQueue(instance.queue);
    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(false));
  }

  @Test
  public void testOnJoinQueueFailed() {
    when(teamMatchmakingService.joinQueue(any())).thenReturn(false);

    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(false));
  }
}
