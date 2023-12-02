package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.PartyBuilder;
import com.faforever.client.builders.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.teammatchmaking.PartyMemberItemController.LEADER_PSEUDO_CLASS;
import static com.faforever.client.teammatchmaking.PartyMemberItemController.PLAYING_PSEUDO_CLASS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartyMemberItemControllerTest extends PlatformTest {

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private ThemeService themeService;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private PartyMemberItemController instance;
  private PlayerBean owner;
  private PlayerBean player;
  private PartyBean party;

  @BeforeEach
  public void setUp() throws Exception {
    party = PartyBuilder.create().defaultValues().get();
    owner = party.getOwner();
    player = PlayerBeanBuilder.create().defaultValues().username("player").id(100).defaultValues().get();
    PartyMember partyMember = PartyMemberBuilder.create(player).defaultValues().get();
    party.getMembers().add(partyMember);
    lenient().when(i18n.get(anyString())).thenReturn("");
    lenient().when(i18n.getOrDefault(anyString(), anyString())).thenReturn("");
    lenient().when(i18n.get("teammatchmaking.inPlacement")).thenReturn("In placement");
    lenient().when(i18n.get(eq("leaderboard.divisionName"), anyString(), anyString())).thenReturn("division V");
    lenient().when(i18n.get(eq("teammatchmaking.gameCount"), anyInt())).thenReturn("GAMES PLAYED: 0");
    lenient().when(playerService.getCurrentPlayer()).thenReturn(owner);
    lenient().when(teamMatchmakingService.getParty()).thenReturn(party);
    lenient().when(leaderboardService.getHighestActiveLeagueEntryForPlayer(player)).thenReturn(
        CompletableFuture.completedFuture(Optional.empty()));

    loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> instance.setMember(partyMember));
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

    runOnFxThreadAndWait(() -> instance.setLeagueInfo());

    assertThat(instance.leagueLabel.getText(), is("DIVISION V"));
    assertTrue(instance.leagueImageView.isVisible());
    verify(leaderboardService).loadDivisionImage(SubdivisionBeanBuilder.create().defaultValues().get().getMediumImageUrl());
  }

  @Test
  public void styleChangeWhenPlayerInGame() {
    assertFalse(instance.playerStatusImageView.isVisible());
    assertFalse(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));

    player.setGame(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusImageView.isVisible());
    assertTrue(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> player.setGame(null));
    assertFalse(instance.playerStatusImageView.isVisible());
    assertFalse(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));
  }

  @Test
  public void testPartyOwnerListener() {
    assertThat(instance.crownLabel.isVisible(), is(false));
    assertThat(instance.kickPlayerButton.isVisible(), is(true));
    assertFalse(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> party.setOwner(player));
    assertThat(instance.crownLabel.isVisible(), is(true));
    assertThat(instance.kickPlayerButton.isVisible(), is(false));
    assertTrue(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> party.setOwner(owner));
    assertThat(instance.crownLabel.isVisible(), is(false));
    assertThat(instance.kickPlayerButton.isVisible(), is(true));
    assertFalse(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));
  }

  @Test
  public void testPlayerPropertyListener() {
    verify(countryFlagService).loadCountryFlag(player.getCountry());
    verify(avatarService).loadAvatar(player.getAvatar());
    assertThat(instance.usernameLabel.getText(), is(player.getUsername()));
    assertThat(instance.gameCountLabel.getText(), is("GAMES PLAYED: 0"));
    assertThat(instance.clanLabel.isVisible(), is(true));
    assertThat(instance.clanLabel.getText(), is(String.format("[%s]", player.getClan())));

    player.setCountry("DE");
    player.setAvatar(AvatarBeanBuilder.create().defaultValues().get());
    player.setClan("");
    player.setUsername("player");
    player.setLeaderboardRatings(new HashMap<>());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getText(), is(player.getUsername()));
    assertThat(instance.gameCountLabel.getText(), is("GAMES PLAYED: 0"));
    assertThat(instance.clanLabel.isVisible(), is(false));
    assertThat(instance.clanLabel.getText(), is(""));
  }

  @Test
  public void testOnKickPlayerButtonClicked() {
    instance.onKickPlayerButtonClicked();

    verify(teamMatchmakingService).kickPlayerFromParty(player);
  }

  @Test
  public void testGetRoot() {
    assertThat(instance.getRoot(), is(instance.playerItemRoot));
  }

  @Test
  public void testFactionLabels() {
    assertThat(instance.uefLabel.isDisabled(), is(false));
    assertThat(instance.aeonLabel.isDisabled(), is(false));
    assertThat(instance.cybranLabel.isDisabled(), is(false));
    assertThat(instance.seraphimLabel.isDisabled(), is(false));
  }
}
