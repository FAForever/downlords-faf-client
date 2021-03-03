package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.teammatchmaking.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static com.faforever.client.teammatchmaking.PartyMemberItemController.LEADER_PSEUDO_CLASS;
import static com.faforever.client.teammatchmaking.PartyMemberItemController.PLAYING_PSEUDO_CLASS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartyMemberItemControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;

  private PartyMemberItemController instance;
  private Player owner;
  private Player player;
  private Party party;

  @Before
  public void setUp() throws Exception {
    party = PartyBuilder.create().defaultValues().get();
    owner = party.getOwner();
    player = PlayerBuilder.create("player").id(100).defaultValues().get();
    PartyMember partyMember = PartyMemberBuilder.create(owner).defaultValues().get();
    party.getMembers().add(partyMember);
    when(i18n.get("leaderboard.divisionName")).thenReturn("division");
    when(i18n.get(eq("teammatchmaking.gameCount"), anyInt())).thenReturn("GAMES PLAYED: 0");
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(teamMatchmakingService.getParty()).thenReturn(party);

    instance = new PartyMemberItemController(countryFlagService, avatarService, playerService, teamMatchmakingService,
        uiService, i18n);
    loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> instance.setMember(partyMember));
  }

  @Test
  public void styleChangeWhenPlayerInGame() {
    assertFalse(instance.playerStatusImageView.isVisible());
    assertFalse(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));

    owner.setGame(GameBuilder.create().defaultValues().status(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusImageView.isVisible());
    assertTrue(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> owner.setGame(null));
    assertFalse(instance.playerStatusImageView.isVisible());
    assertFalse(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));
  }

  @Test
  public void testPartyOwnerListener() {
    assertThat(instance.crownLabel.isVisible(), is(true));
    assertThat(instance.kickPlayerButton.isVisible(), is(false));
    assertTrue(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> party.setOwner(player));
    assertThat(instance.crownLabel.isVisible(), is(false));
    assertThat(instance.kickPlayerButton.isVisible(), is(true));
    assertFalse(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> party.setOwner(owner));
    assertThat(instance.crownLabel.isVisible(), is(true));
    assertThat(instance.kickPlayerButton.isVisible(), is(false));
    assertTrue(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));
  }

  @Test
  public void testPlayerPropertyListener() {
    verify(countryFlagService).loadCountryFlag(owner.getCountry());
    verify(avatarService).loadAvatar(owner.getAvatarUrl());
    assertThat(instance.usernameLabel.getText(), is(owner.getUsername()));
    assertThat(instance.gameCountLabel.getText(), is("GAMES PLAYED: 0"));
    assertThat(instance.clanLabel.isVisible(), is(true));
    assertThat(instance.clanLabel.getText(), is(String.format("[%s]", player.getClan())));

    owner.setCountry("DE");
    owner.setAvatarUrl("");
    owner.setClan("");
    owner.setNumberOfGames(10);
    owner.setUsername("player");
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getText(), is(owner.getUsername()));
    assertThat(instance.gameCountLabel.getText(), is("GAMES PLAYED: 0"));
    assertThat(instance.clanLabel.isVisible(), is(false));
    assertThat(instance.clanLabel.getText(), is(""));
  }

  @Test
  public void testOnKickPlayerButtonClicked() {
    instance.onKickPlayerButtonClicked(null);

    verify(teamMatchmakingService).kickPlayerFromParty(owner);
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
