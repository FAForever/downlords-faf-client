package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserServiceTest extends ServiceTest {
  private static final String CHANNEL_NAME = "testChannel";

  @InjectMocks
  private ChatUserService instance;
  @Mock
  private AvatarService avatarService;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private MapService mapService;

  private PlayerBean player;
  private AvatarBean avatar;
  private Preferences preferences;
  private ChatChannelUser chatUser;
  private ClanBean testClan;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().get();
    avatar = AvatarBeanBuilder.create().defaultValues().get();
    chatUser = ChatChannelUserBuilder.create(player.getUsername(), CHANNEL_NAME).defaultValues().get();
    preferences = PreferencesBuilder.create().defaultValues()
        .chatPrefs()
        .chatColorMode(ChatColorMode.DEFAULT)
        .then()
        .get();
    testClan = ClanBeanBuilder.create().defaultValues().get();
    when(countryFlagService.loadCountryFlag(anyString())).thenReturn(Optional.of(mock(Image.class)));
    when(uiService.getThemeImage(anyString())).thenReturn(mock(Image.class));
    when(mapService.loadPreview(anyString(), any(PreviewSize.class))).thenReturn(mock(Image.class));
    when(avatarService.loadAvatar(any(AvatarBean.class))).thenReturn(mock(Image.class));
    when(i18n.getCountryNameLocalized("US")).thenReturn("United States");
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance.bindChatUserPlayerProperties(chatUser);
  }

  @Test
  public void testPlayerIsNull() {
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(any());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
  }

  @Test
  public void testClanNotNull() {
    player.setClan(testClan.getTag());
    chatUser.setPlayer(player);

    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanChange() {
    ClanBean newClan = new ClanBean();
    newClan.setTag("NC");
    player.setClan(testClan.getTag());
    chatUser.setPlayer(player);
    player.setClan(newClan.getTag());

    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", newClan.getTag()));
  }

  @Test
  public void testClanSameChange() {
    player.setClan(testClan.getTag());
    chatUser.setPlayer(player);
    player.setClan(testClan.getTag());

    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanNull() {
    player.setClan(null);
    chatUser.setPlayer(player);

    assertTrue(chatUser.getClanTag().isEmpty());
  }

  @Test
  public void testAvatarNotNull() {
    player.setAvatar(avatar);
    chatUser.setPlayer(player);

    assertTrue(chatUser.getAvatar().isPresent());
    verify(avatarService).loadAvatar(avatar);
  }

  @Test
  public void testAvatarChange() throws MalformedURLException {
    player.setAvatar(avatar);
    chatUser.setPlayer(player);
    player.setAvatar(avatar);

    assertTrue(chatUser.getAvatar().isPresent());
    verify(avatarService).loadAvatar(avatar);
  }

  @Test
  public void testAvatarSameChange() {
    player.setAvatar(avatar);
    chatUser.setPlayer(player);
    player.setAvatar(avatar);

    assertTrue(chatUser.getAvatar().isPresent());
    verify(avatarService).loadAvatar(avatar);
  }

  @Test
  public void testAvatarNull() {
    player.setAvatar(null);
    chatUser.setPlayer(player);

    assertTrue(chatUser.getAvatar().isEmpty());
    verify(avatarService, never()).loadAvatar(any(AvatarBean.class));
  }

  @Test
  public void testCountryNotNull() {
    player.setCountry("US");
    chatUser.setPlayer(player);

    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("United States", chatUser.getCountryName().orElse(null));
    verify(countryFlagService).loadCountryFlag("US");
  }

  @Test
  public void testCountryNull() {
    player.setCountry(null);
    chatUser.setPlayer(player);

    assertTrue(chatUser.getCountryFlag().isEmpty());
    assertTrue(chatUser.getCountryName().isEmpty());
    verify(countryFlagService, never()).loadCountryFlag(any());
  }

  @Test
  public void testStatusToIdle() {
    player.setGame(null);
    when(i18n.get("game.gameStatus.idle")).thenReturn("Idle");
    chatUser.setPlayer(player);

    assertFalse(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.IDLE, chatUser.getPlayerStatus().orElse(null));
    assertEquals("Idle", chatUser.getStatusTooltipText().orElse(null));
    verify(uiService, never()).getThemeImage(anyString());
  }

  @Test
  public void testStatusToPlaying() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.playing")).thenReturn("Playing");
    chatUser.setPlayer(player);

    assertTrue(chatUser.getMapImage().isPresent());
    assertTrue(chatUser.getGameStatusImage().isPresent());
    assertEquals(PlayerStatus.PLAYING, chatUser.getPlayerStatus().orElse(null));
    assertEquals("Playing", chatUser.getStatusTooltipText().orElse(null));
    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
  }

  @Test
  public void testStatusToHosting() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).host(player.getUsername()).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.hosting")).thenReturn("Hosting");
    chatUser.setPlayer(player);

    assertTrue(chatUser.getGameStatusImage().isPresent());
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.HOSTING, chatUser.getPlayerStatus().orElse(null));
    assertEquals("Hosting", chatUser.getStatusTooltipText().orElse(null));
    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
  }

  @Test
  public void testStatusToLobbying() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.lobby")).thenReturn("Waiting for game to start");
    chatUser.setPlayer(player);

    assertTrue(chatUser.getMapImage().isPresent());
    assertTrue(chatUser.getGameStatusImage().isPresent());
    assertEquals(PlayerStatus.LOBBYING, chatUser.getPlayerStatus().orElse(null));
    assertEquals("Waiting for game to start", chatUser.getStatusTooltipText().orElse(null));
    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
  }

  @Test
  public void testUserColorNotSet() {
    chatUser.setPlayer(null);

    assertTrue(chatUser.getColor().isEmpty());
  }

  @Test
  public void testUserColorSetNoPlayer() {
    preferences.getChat().setUserToColor(FXCollections.observableMap(Map.of("junit", Color.AQUA)));
    chatUser.setPlayer(null);
    instance.populateColor(chatUser);

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testGroupColorSet() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.AQUA)));
    player.setSocialStatus(SocialStatus.FRIEND);
    chatUser.setPlayer(player);
    instance.populateColor(chatUser);

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testModeratorColorOverGroup() {
    preferences.getChat()
        .setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.MODERATOR, Color.RED, ChatUserCategory.FRIEND, Color.AQUA)));
    chatUser.setModerator(true);
    player.setSocialStatus(SocialStatus.FRIEND);
    chatUser.setPlayer(player);
    instance.populateColor(chatUser);

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.RED);
  }
}
