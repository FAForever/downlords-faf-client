package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.game.Game;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
  @Mock
  private PartyMember partyMember;

  private static final PseudoClass PLAYING_PSEUDO_CLASS = PseudoClass.getPseudoClass("playing");
  private PartyMemberItemController instance;
  private Player player;

  @Before
  public void setUp() throws Exception{
    player = new Player("tester");
    when(partyMember.getPlayer()).thenReturn(player);
    when(i18n.get("leaderboard.divisionName", RatingUtil.getLeaderboardRating(player))).thenReturn("In Placement");
    when(i18n.get("teammatchmaking.gameCount", player.getNumberOfGames())).thenReturn("Games played: 0");
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<Player>());
    Party party = new Party();
    party.setOwner(player);
    when(teamMatchmakingService.getParty()).thenReturn(party);

    instance = new PartyMemberItemController(countryFlagService, avatarService, playerService, teamMatchmakingService,
        uiService, i18n);
    loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml", clazz -> instance);
    instance.setMember(partyMember);
  }

  @Test
  public void styleChangeWhenPlayerInGame() {
    player.setGame(new Game());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusImageView.isManaged());
    assertTrue(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));
  }

  @Test
  public void styleChangeWhenPlayerNotInGame() {
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.playerStatusImageView.isManaged());
    assertFalse(instance.playerCard.getPseudoClassStates().contains(PLAYING_PSEUDO_CLASS));
  }
}
