package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.helper.TooltipHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.Faction;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class PlayerCardControllerTest extends PlatformTest {
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  private PlayerCardController instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new PlayerCardController(uiService, countryFlagService, avatarService, leaderboardService, contextMenuBuilder, i18n);

    when(uiService.getImage(ThemeService.RANDOM_FACTION_IMAGE)).thenReturn(
        new Image(ThemeService.RANDOM_FACTION_IMAGE));
    loadFxml("theme/player_card.fxml", clazz -> instance);
  }

  @Test
  public void testSetFoe() {
    when(i18n.get("userInfo.tooltipFormat.noRating", "foe")).thenReturn("foe");

    PlayerInfoBuilder playerInfoBuilder = PlayerInfoBuilder.create()
                                                           .defaultValues()
                                                           .username("foe")
                                                           .socialStatus(SocialStatus.FOE);
    PlayerInfo player = playerInfoBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.CYBRAN);

    assertTrue(instance.factionIcon.getStyleClass().contains(ThemeService.CYBRAN_STYLE_CLASS));
    assertTrue(instance.factionIcon.isVisible());
    assertFalse(instance.factionImage.isVisible());
    assertTrue(instance.foeIconText.isVisible());
    assertFalse(instance.friendIconText.isVisible());
    assertEquals("foe", instance.playerInfo.getText());
  }

  @Test
  public void testSetFriend() {
    when(i18n.get("userInfo.tooltipFormat.noRating", "user")).thenReturn("user");

    PlayerInfoBuilder playerInfoBuilder = PlayerInfoBuilder.create()
                                                           .defaultValues()
                                                           .username("user")
                                                           .socialStatus(SocialStatus.FRIEND);
    PlayerInfo player = playerInfoBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.SERAPHIM);

    assertTrue(instance.factionIcon.getStyleClass().contains(ThemeService.SERAPHIM_STYLE_CLASS));
    assertTrue(instance.factionIcon.isVisible());
    assertFalse(instance.factionImage.isVisible());
    assertFalse(instance.foeIconText.isVisible());
    assertTrue(instance.friendIconText.isVisible());
    assertEquals("user", instance.playerInfo.getText());
  }

  @Test
  public void testSetOther() {
    when(i18n.get("userInfo.tooltipFormat.noRating", "user")).thenReturn("user");

    PlayerInfoBuilder playerInfoBuilder = PlayerInfoBuilder.create()
                                                           .defaultValues()
                                                           .username("user")
                                                           .socialStatus(SocialStatus.OTHER);
    PlayerInfo player = playerInfoBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.factionImage.getImage().getUrl().contains(ThemeService.RANDOM_FACTION_IMAGE));
    assertFalse(instance.factionIcon.isVisible());
    assertTrue(instance.factionImage.isVisible());
    assertFalse(instance.foeIconText.isVisible());
    assertFalse(instance.friendIconText.isVisible());
    assertEquals("user", instance.playerInfo.getText());
  }

  @Test
  public void testSetPlayerAvatar() {
    Image avatarImage = new Image(InputStream.nullInputStream());
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(avatarImage);

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.avatarImageView.isVisible());
    assertTrue(instance.avatarStackPane.isVisible());
    assertEquals(avatarImage, instance.avatarImageView.getImage());
  }

  @Test
  public void testInvisibleAvatarImageView() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().avatar(null).get();

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertFalse(instance.avatarImageView.isVisible());
    assertTrue(instance.avatarStackPane.isVisible());
  }

  @Test
  public void testSetCountryImage() {
    Image countryImage = new Image(InputStream.nullInputStream());
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(countryFlagService.loadCountryFlag(player.getCountry())).thenReturn(Optional.of(countryImage));

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.countryImageView.isVisible());
    assertEquals(countryImage, instance.countryImageView.getImage());
  }

  @Test
  public void testInvisibleCountryImageView() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(countryFlagService.loadCountryFlag(player.getCountry())).thenReturn(Optional.empty());

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertFalse(instance.countryImageView.isVisible());
  }

  @Test
  public void testNotePlayerTooltip() {
    PlayerInfo player = PlayerInfoBuilder.create()
                                         .defaultValues()
                                         .note("Player 1")
                                         .game(GameInfoBuilder.create().defaultValues().get())
                                         .get();
    instance.setPlayer(player);
    instance.setRating(0);
    instance.setFaction(Faction.AEON);
    assertEquals("Player 1", TooltipHelper.getTooltipText(instance.root));

    runOnFxThreadAndWait(() -> player.setNote(""));
    assertNull(TooltipHelper.getTooltip(instance.root));
  }

  @Test 
  public void testInvisibleAvatarOnRemove() {
    Image avatarImage = new Image(InputStream.nullInputStream());
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(avatarImage);

    instance.setPlayer(player);
    instance.setFaction(Faction.RANDOM);
    instance.removeAvatar();

    assertFalse(instance.avatarStackPane.isVisible());
  }
}