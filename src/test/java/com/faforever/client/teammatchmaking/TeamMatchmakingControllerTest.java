package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamMatchmakingControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private FafService fafService;
  @Mock
  private EventBus eventBus;

  private Preferences preferences;
  private TeamMatchmakingController instance;

  @Before
  public void setUp() throws Exception {
    Player player = new Player("tester");
    player.setId(1);
    prepareParty(player);
    ObservableList<Faction> factionList = FXCollections.observableArrayList(Faction.SERAPHIM, Faction.AEON);

    preferences = PreferencesBuilder.create().defaultValues()
        .matchmakerPrefs()
        .factions(factionList)
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(anyString(), any(Object.class))).thenReturn("");
    when(teamMatchmakingService.currentlyInQueueProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(teamMatchmakingService.queuesReadyForUpdateProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(teamMatchmakingService.getPlayersInGame()).thenReturn(FXCollections.observableSet());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<Player>());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    when(i18n.get(anyString())).thenReturn("");
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml")).thenAnswer(invocation -> {
      MatchmakingChatController controller = mock(MatchmakingChatController.class);
      when(controller.getRoot()).thenReturn(new Tab());
      return controller;
    });
    instance = new TeamMatchmakingController(countryFlagService, avatarService, preferencesService, playerService, i18n, uiService,
        teamMatchmakingService, fafService, eventBus);
    loadFxml("theme/play/team_matchmaking.fxml", clazz -> instance);
  }

  private void prepareParty(Player player) {
    Party party = new Party();
    party.setOwner(player);
    PartyMember member = new PartyMember(player);
    ObservableList<PartyMember> members = FXCollections.observableArrayList();
    members.add(member);
    party.setMembers(members);
    when(teamMatchmakingService.getParty()).thenReturn(party);
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Faction>> captor = ArgumentCaptor.forClass(List.class);

    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
    verify(teamMatchmakingService).sendFactionSelection(captor.capture());
    assertThat(captor.getValue(), containsInAnyOrder(Faction.SERAPHIM, Faction.AEON));
  }

  @Test
  public void testOnInvitePlayerButtonClicked() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml")).thenAnswer(invocation -> {
      InvitePlayerController controller = mock(InvitePlayerController.class);
      return controller;
    });

    instance.onInvitePlayerButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).showInDialog(instance.teamMatchmakingRoot, null, "");
  }

  @Test
  public void testOnLeavePartyButtonClicked() {
    instance.onLeavePartyButtonClicked();

    verify(teamMatchmakingService).leaveParty();
  }

  @Test
  public void testOnFactionButtonClicked() {
    instance.uefButton.setSelected(true);
    instance.aeonButton.setSelected(true);
    instance.cybranButton.setSelected(false);
    instance.seraphimButton.setSelected(false);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Faction>> captor = ArgumentCaptor.forClass(List.class);

    instance.onFactionButtonClicked();

    assertThat(preferences.getMatchmaker().getFactions(), containsInAnyOrder(Faction.UEF, Faction.AEON));
    // First invocation happens in initialize()
    verify(teamMatchmakingService, times(2)).sendFactionSelection(captor.capture());
    assertThat(captor.getValue(), containsInAnyOrder(Faction.UEF, Faction.AEON));
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testOnFactionButtonClickedWhileNoFactionsSelected() {
    instance.uefButton.setSelected(false);
    instance.aeonButton.setSelected(false);
    instance.cybranButton.setSelected(false);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    verify(preferencesService, never()).storeInBackground();
    assertThat(instance.uefButton.isSelected(), is(true));
    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.cybranButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
  }

  @Test
  public void testOnPartyChatMessage() {
    ChatMessage message = mock(ChatMessage.class);
    when(message.getSource()).thenReturn("#tester'sParty");
    when(instance.matchmakingChatController.getReceiver()).thenReturn("#tester'sParty");

    instance.onChatMessage(new ChatMessageEvent(message));
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance.matchmakingChatController).onChatMessage(message);
  }

  @Test
  public void testOnOtherChatMessage() {
    ChatMessage message = mock(ChatMessage.class);
    when(message.getSource()).thenReturn("#other'sParty");
    when(instance.matchmakingChatController.getReceiver()).thenReturn("#tester'sParty");

    instance.onChatMessage(new ChatMessageEvent(message));
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance.matchmakingChatController, never()).onChatMessage(message);
  }

  @Test
  public void testOnQueuesAdded() {
    ObservableList<MatchmakingQueue> queues = FXCollections.observableArrayList();
    queues.addAll(new MatchmakingQueue(), new MatchmakingQueue());
    when(teamMatchmakingService.getMatchmakingQueues()).thenReturn(queues);
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml")).thenAnswer(invocation -> {
      MatchmakingQueueItemController controller = mock(MatchmakingQueueItemController.class);
      when(controller.getRoot()).thenReturn(new VBox());
      return controller;
    });

    teamMatchmakingService.queuesReadyForUpdateProperty().set(true);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.queueBox.getChildren().size(), is(2));
  }
}
