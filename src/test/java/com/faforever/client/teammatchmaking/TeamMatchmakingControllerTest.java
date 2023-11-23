package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.PartyBuilder;
import com.faforever.client.builders.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.Faction;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.teammatchmaking.PartyMemberItemController.LEADER_PSEUDO_CLASS;
import static com.faforever.client.teammatchmaking.TeamMatchmakingController.CHAT_AT_BOTTOM_PSEUDO_CLASS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class TeamMatchmakingControllerTest extends PlatformTest {

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private LeaderboardService leaderboardService;

  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private ChatService chatService;
  @Mock
  private MatchmakingChatController matchmakingChatController;
  @Spy
  private MatchmakerPrefs matchmakerPrefs;
  @InjectMocks
  private TeamMatchmakingController instance;
  private PlayerBean player;
  private PartyBean party;
  private ObservableList<MatchmakerQueueBean> matchmakerQueues;

  @BeforeEach
  public void setUp() throws Exception {
    party = PartyBuilder.create().defaultValues().get();
    player = party.getOwner();
    matchmakerQueues = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    matchmakerPrefs.getFactions().setAll(List.of(Faction.SERAPHIM, Faction.AEON));

    when(teamMatchmakingService.getParty()).thenReturn(party);

    when(i18n.get(anyString(), any(Object.class))).thenReturn("");
    when(leaderboardService.getHighestActiveLeagueEntryForPlayer(player)).thenReturn(
        CompletableFuture.completedFuture(Optional.empty()));
    when(teamMatchmakingService.inQueueProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(teamMatchmakingService.searchingProperty()).thenReturn(new SimpleBooleanProperty());
    when(teamMatchmakingService.partyMembersNotReadyProperty()).thenReturn(new ReadOnlyBooleanWrapper());
    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(false);
    when(teamMatchmakingService.anyQueueSelectedProperty()).thenReturn(new SimpleBooleanProperty().not());
    when(teamMatchmakingService.isAnyQueueSelected()).thenReturn(false);
    when(teamMatchmakingService.getQueues()).thenReturn(matchmakerQueues);
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("");
    when(i18n.get("teammatchmaking.inPlacement")).thenReturn("In Placement");
    when(i18n.get(eq("leaderboard.divisionName"), anyString(), anyString())).thenReturn("division V");
    when(matchmakingChatController.getRoot()).thenReturn(new Tab());
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml")).thenAnswer(invocation -> matchmakingChatController);

    loadFxml("theme/play/team_matchmaking.fxml", clazz -> {
      if (clazz == MatchmakingChatController.class) {
        return matchmakingChatController;
      }

      return instance;
    });
  }

  @Test
  public void testLeagueNotSet() {
    assertFalse(instance.leagueImageView.isVisible());
    assertThat(instance.leagueLabel.getText(), is("IN PLACEMENT"));
  }

  @Test
  public void testLeagueSet() {
    when(leaderboardService.getHighestActiveLeagueEntryForPlayer(player)).thenReturn(
        CompletableFuture.completedFuture(Optional.of(LeagueEntryBeanBuilder.create().defaultValues().get())));

    reinitialize(instance);
    waitForFxEvents();

    assertTrue(instance.leagueImageView.isVisible());
    assertThat(instance.leagueLabel.getText(), is("DIVISION V"));
    verify(leaderboardService).loadDivisionImage(SubdivisionBeanBuilder.create().defaultValues().get().getMediumImageUrl());
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() {
    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
  }

  @Test
  public void testOnInvitePlayerButtonClicked() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml")).thenAnswer(invocation -> {
      return mock(InvitePlayerController.class);
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

    instance.onFactionButtonClicked();

    assertThat(matchmakerPrefs.getFactions(), containsInAnyOrder(Faction.UEF, Faction.AEON));
  }

  @Test
  public void testOnFactionButtonClickedWhileNoFactionsSelected() {
    instance.uefButton.setSelected(false);
    instance.aeonButton.setSelected(false);
    instance.cybranButton.setSelected(false);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.cybranButton.isSelected(), is(false));
    assertThat(instance.seraphimButton.isSelected(), is(true));
  }

  @Test
  @Disabled("I will deal with this later")
  public void testQueueHeadingListener() {
    verify(i18n, times(1)).get("teammatchmaking.playerTitle");

    when(teamMatchmakingService.isAnyQueueSelected()).thenReturn(false);
    runOnFxThreadAndWait(() -> party.setOwner(PlayerBeanBuilder.create().defaultValues().get()));
    verify(i18n).get("teammatchmaking.searchButton.noQueueSelected");

    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(true);
    runOnFxThreadAndWait(() -> {
      party.setOwner(PlayerBeanBuilder.create().defaultValues().username("test").id(100).get());
      party.setOwner(player);
    });
    verify(i18n).get("teammatchmaking.searchButton.memberInGame");

    runOnFxThreadAndWait(() -> party.setOwner(PlayerBeanBuilder.create()
        .defaultValues()
        .username("test")
        .id(100)
        .get()));
    verify(i18n, times(2)).get("teammatchmaking.searchButton.inParty");

    when(teamMatchmakingService.isInQueue()).thenReturn(true);
    runOnFxThreadAndWait(() -> party.setOwner(player));
    verify(i18n).get("teammatchmaking.searchButton.inQueue");
  }

  @Test
  public void testOnQueuesAdded() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml")).thenAnswer(invocation -> {
      MatchmakingQueueItemController controller = mock(MatchmakingQueueItemController.class);
      when(controller.getRoot()).thenReturn(new VBox());
      return controller;
    });

    matchmakerQueues.add(MatchmakerQueueBeanBuilder.create().defaultValues().id(1).get());
    matchmakerQueues.add(MatchmakerQueueBeanBuilder.create().defaultValues().id(2).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.queuePane.getChildren().size(), is(2));

    matchmakerQueues.add(MatchmakerQueueBeanBuilder.create().defaultValues().id(3).get());
    matchmakerQueues.add(MatchmakerQueueBeanBuilder.create().defaultValues().id(4).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.queuePane.getChildren().size(), is(4));
  }

  @Test
  public void testOnPartyMembersAdded() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml")).thenAnswer(invocation -> {
      PartyMemberItemController controller = mock(PartyMemberItemController.class);
      when(controller.getRoot()).thenReturn(new AnchorPane());
      return controller;
    });

    assertFalse(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username("m1").id(101).get();
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username("m2").id(102).get();
    party.getMembers().add(PartyMemberBuilder.create(player1).defaultValues().get());
    party.getMembers().add(PartyMemberBuilder.create(player2).defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));
    assertThat(instance.partyMemberPane.getChildren().size(), is(2));

    PlayerBean player3 = PlayerBeanBuilder.create().defaultValues().username("m3").id(103).get();
    party.getMembers().add(PartyMemberBuilder.create(player3).defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.partyMemberPane.getChildren().size(), is(3));

    party.getMembers().remove(3);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.partyMemberPane.getChildren().size(), is(2));
  }

  @Test
  public void testGetRoot() {
    assertThat(instance.getRoot(), is(instance.teamMatchmakingRoot));
  }

  @Test
  public void testDynamicChatListener() {
    runOnFxThreadAndWait(() -> instance.contentPane.resize(2000, 1000));
    assertFalse(instance.chatTabPane.getPseudoClassStates().contains(CHAT_AT_BOTTOM_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> instance.contentPane.resize(1000, 1000));
    assertTrue(instance.chatTabPane.getPseudoClassStates().contains(CHAT_AT_BOTTOM_PSEUDO_CLASS));
  }
}
