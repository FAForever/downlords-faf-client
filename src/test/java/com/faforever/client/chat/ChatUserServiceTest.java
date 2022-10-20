package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.clan.ClanService;
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
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
  private ClanService clanService;
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
    when(clanService.getClanByTag(testClan.getTag())).thenReturn(CompletableFuture.completedFuture(Optional.of(testClan)));
    when(countryFlagService.loadCountryFlag(anyString())).thenReturn(Optional.of(mock(Image.class)));
    when(uiService.getThemeImage(anyString())).thenReturn(mock(Image.class));
    when(mapService.loadPreview(anyString(), any(PreviewSize.class))).thenReturn(mock(Image.class));
    when(avatarService.loadAvatar(any(AvatarBean.class))).thenReturn(mock(Image.class));
    when(i18n.getCountryNameLocalized("US")).thenReturn("United States");
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testPlayerIsNull() {
    instance.associatePlayerToChatUser(chatUser, null);


    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(any());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
    assertNull(chatUser.getAvatarChangeListener());
    assertNull(chatUser.getSocialStatusChangeListener());
    assertNull(chatUser.getClanTagChangeListener());
    assertNull(chatUser.getCountryInvalidationListener());
    assertNull(chatUser.getGameStatusChangeListener());
    assertNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testChatUserNotDisplayed() {
    chatUser.setDisplayed(false);
    instance.associatePlayerToChatUser(chatUser, player);


    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(any());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
    assertNull(chatUser.getAvatarChangeListener());
    assertNull(chatUser.getSocialStatusChangeListener());
    assertNull(chatUser.getClanTagChangeListener());
    assertNull(chatUser.getCountryInvalidationListener());
    assertNull(chatUser.getGameStatusChangeListener());
    assertNotNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testChatUserNotDisplayedToDisplayed() {
    chatUser.setDisplayed(false);
    instance.associatePlayerToChatUser(chatUser, player);
    chatUser.setDisplayed(true);


    verify(clanService, times(2)).getClanByTag(anyString());
    verify(countryFlagService, times(2)).loadCountryFlag(anyString());
    verify(avatarService, times(2)).loadAvatar(any());
    assertNotNull(chatUser.getAvatarChangeListener());
    assertNotNull(chatUser.getSocialStatus());
    assertNotNull(chatUser.getClanTagChangeListener());
    assertNotNull(chatUser.getCountryInvalidationListener());
    assertNotNull(chatUser.getGameStatusChangeListener());
    assertNotNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testChatUserDisplayedToNotDisplayed() {
    chatUser.setDisplayed(true);
    instance.associatePlayerToChatUser(chatUser, player);

    chatUser.setDisplayed(false);


    verify(clanService, times(3)).getClanByTag(anyString());
    verify(countryFlagService, times(3)).loadCountryFlag(anyString());
    verify(avatarService, times(3)).loadAvatar(any());
    assertTrue(chatUser.getStatusTooltipText().isEmpty());
    assertTrue(chatUser.getGameStatusImage().isEmpty());
    assertTrue(chatUser.getMapImage().isEmpty());
    assertTrue(chatUser.getCountryFlag().isEmpty());
    assertTrue(chatUser.getCountryName().isEmpty());
    assertTrue(chatUser.getClan().isEmpty());
    assertTrue(chatUser.getAvatar().isEmpty());
  }

  @Test
  public void testListenersRemovedOnSecondAssociation() {
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username("junit1").get();
    instance.associatePlayerToChatUser(chatUser, player1);


    ChangeListener<PlayerStatus> gameStatusListener = chatUser.getGameStatusChangeListener();
    ChangeListener<SocialStatus> socialStatusListener = chatUser.getSocialStatusChangeListener();
    ChangeListener<String> clanTagListener = chatUser.getClanTagChangeListener();
    ChangeListener<AvatarBean> avatarListener = chatUser.getAvatarChangeListener();
    ChangeListener<String> countryListener = chatUser.getCountryInvalidationListener();
    InvalidationListener displayedListener = chatUser.getDisplayedChangeListener();

    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username("junit2").get();
    instance.associatePlayerToChatUser(chatUser, player2);


    assertNotEquals(gameStatusListener, chatUser.getGameStatusChangeListener());
    assertNotEquals(socialStatusListener, chatUser.getSocialStatusChangeListener());
    assertNotEquals(clanTagListener, chatUser.getClanTagChangeListener());
    assertNotEquals(avatarListener, chatUser.getAvatarChangeListener());
    assertNotEquals(countryListener, chatUser.getCountryInvalidationListener());
    assertNotEquals(displayedListener, chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testListenersRemovedOnNullAssociation() {
    instance.associatePlayerToChatUser(chatUser, player);


    assertNotNull(chatUser.getGameStatusChangeListener());
    assertNotNull(chatUser.getSocialStatusChangeListener());
    assertNotNull(chatUser.getClanTagChangeListener());
    assertNotNull(chatUser.getAvatarChangeListener());
    assertNotNull(chatUser.getCountryInvalidationListener());
    assertNotNull(chatUser.getDisplayedChangeListener());

    instance.associatePlayerToChatUser(chatUser, null);


    assertNull(chatUser.getGameStatusChangeListener());
    assertNull(chatUser.getSocialStatusChangeListener());
    assertNull(chatUser.getClanTagChangeListener());
    assertNull(chatUser.getAvatarChangeListener());
    assertNull(chatUser.getCountryInvalidationListener());
    assertNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testClanNotNull() {
    player.setClan(testClan.getTag());
    instance.associatePlayerToChatUser(chatUser, player);


    verify(clanService, times(3)).getClanByTag(testClan.getTag());
    assertEquals(chatUser.getClan().orElse(null), testClan);
    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanChange() {
    ClanBean newClan = new ClanBean();
    newClan.setTag("NC");
    when(clanService.getClanByTag(newClan.getTag())).thenReturn(CompletableFuture.completedFuture(Optional.of(newClan)));
    player.setClan(testClan.getTag());
    instance.associatePlayerToChatUser(chatUser, player);
    player.setClan(newClan.getTag());


    verify(clanService, times(3)).getClanByTag(testClan.getTag());
    verify(clanService, times(1)).getClanByTag(newClan.getTag());
    assertEquals(chatUser.getClan().orElse(null), newClan);
    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", newClan.getTag()));
  }

  @Test
  public void testClanSameChange() {
    player.setClan(testClan.getTag());
    instance.associatePlayerToChatUser(chatUser, player);
    player.setClan(testClan.getTag());


    verify(clanService, times(3)).getClanByTag(testClan.getTag());
    assertEquals(chatUser.getClan().orElse(null), testClan);
    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanNull() {
    player.setClan(null);
    instance.associatePlayerToChatUser(chatUser, player);


    verify(clanService, never()).getClanByTag(anyString());
    assertTrue(chatUser.getClan().isEmpty());
    assertTrue(chatUser.getClanTag().isEmpty());
  }

  @Test
  public void testAvatarNotNull() {
    player.setAvatar(avatar);
    instance.associatePlayerToChatUser(chatUser, player);


    verify(avatarService, times(3)).loadAvatar(avatar);
    assertTrue(chatUser.getAvatar().isPresent());
  }

  @Test
  public void testAvatarChange() throws MalformedURLException {
    player.setAvatar(avatar);
    instance.associatePlayerToChatUser(chatUser, player);
    player.setAvatar(avatar);


    verify(avatarService, times(3)).loadAvatar(avatar);
    assertTrue(chatUser.getAvatar().isPresent());
  }

  @Test
  public void testAvatarSameChange() {
    player.setAvatar(avatar);
    instance.associatePlayerToChatUser(chatUser, player);
    player.setAvatar(avatar);


    verify(avatarService, times(3)).loadAvatar(avatar);
    assertTrue(chatUser.getAvatar().isPresent());
  }

  @Test
  public void testAvatarNull() {
    player.setAvatar(null);
    instance.associatePlayerToChatUser(chatUser, player);


    verify(avatarService, never()).loadAvatar(any(AvatarBean.class));
    assertTrue(chatUser.getAvatar().isEmpty());
  }

  @Test
  public void testCountryNotNull() {
    player.setCountry("US");
    instance.associatePlayerToChatUser(chatUser, player);


    verify(countryFlagService, times(3)).loadCountryFlag("US");
    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("United States", chatUser.getCountryName().orElse(null));
  }

  @Test
  public void testCountryChange() {
    when(i18n.getCountryNameLocalized("DE")).thenReturn("Germany");

    player.setCountry("US");
    instance.associatePlayerToChatUser(chatUser, player);
    player.setCountry("DE");


    verify(countryFlagService, times(3)).loadCountryFlag("US");
    verify(countryFlagService).loadCountryFlag("DE");
    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("Germany", chatUser.getCountryName().orElse(null));
  }

  @Test
  public void testCountrySameChange() {
    player.setCountry("US");
    instance.associatePlayerToChatUser(chatUser, player);
    player.setCountry("US");


    verify(countryFlagService, times(3)).loadCountryFlag("US");
    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("United States", chatUser.getCountryName().orElse(null));
  }

  @Test
  public void testCountryNull() {
    player.setCountry(null);
    instance.associatePlayerToChatUser(chatUser, player);


    verify(countryFlagService).loadCountryFlag(null);
    assertTrue(chatUser.getCountryFlag().isEmpty());
    assertTrue(chatUser.getCountryName().isEmpty());
  }

  @Test
  public void testStatusToIdle() {
    player.setGame(null);
    when(i18n.get("game.gameStatus.idle")).thenReturn("Idle");
    instance.associatePlayerToChatUser(chatUser, player);


    verify(uiService, never()).getThemeImage(anyString());
    assertFalse(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.IDLE, chatUser.getGameStatus().orElse(null));
    assertEquals("Idle", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToPlaying() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.playing")).thenReturn("Playing");
    instance.associatePlayerToChatUser(chatUser, player);


    verify(uiService, times(3)).getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.PLAYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Playing", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToHosting() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).host(player.getUsername()).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.hosting")).thenReturn("Hosting");
    instance.associatePlayerToChatUser(chatUser, player);


    verify(uiService, times(3)).getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.HOSTING, chatUser.getGameStatus().orElse(null));
    assertEquals("Hosting", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToLobbying() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.lobby")).thenReturn("Waiting for game to start");
    instance.associatePlayerToChatUser(chatUser, player);


    verify(uiService, times(3)).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.LOBBYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Waiting for game to start", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusChange() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.lobby")).thenReturn("Waiting for game to start");
    when(i18n.get("game.gameStatus.playing")).thenReturn("Playing");
    instance.associatePlayerToChatUser(chatUser, player);
    game.setStatus(GameStatus.PLAYING);


    verify(uiService, times(3)).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.PLAYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Playing", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusSameChange() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).get();
    player.setGame(game);
    when(i18n.get("game.gameStatus.lobby")).thenReturn("Waiting for game to start");
    instance.associatePlayerToChatUser(chatUser, player);
    game.setStatus(GameStatus.OPEN);


    verify(uiService, times(3)).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.LOBBYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Waiting for game to start", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testUserColorNotSet() {
    instance.associatePlayerToChatUser(chatUser, null);


    assertTrue(chatUser.getColor().isEmpty());
  }

  @Test
  public void testUserColorSetNoPlayer() {
    preferences.getChat().setUserToColor(FXCollections.observableMap(Map.of("junit", Color.AQUA)));
    instance.associatePlayerToChatUser(chatUser, null);


    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testGroupColorSet() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.AQUA)));
    player.setSocialStatus(SocialStatus.FRIEND);
    instance.associatePlayerToChatUser(chatUser, player);


    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testGroupChange() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.AQUA)));
    player.setSocialStatus(SocialStatus.FRIEND);
    instance.associatePlayerToChatUser(chatUser, player);
    player.setSocialStatus(SocialStatus.OTHER);


    assertFalse(chatUser.getColor().isPresent());
  }

  @Test
  public void testGroupSameChange() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.AQUA)));
    player.setSocialStatus(SocialStatus.FRIEND);
    instance.associatePlayerToChatUser(chatUser, player);
    player.setSocialStatus(SocialStatus.FRIEND);


    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testModeratorColorOverGroup() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.MODERATOR, Color.RED, ChatUserCategory.FRIEND, Color.AQUA)));
    chatUser.setModerator(true);
    player.setSocialStatus(SocialStatus.FRIEND);
    instance.associatePlayerToChatUser(chatUser, player);


    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.RED);
  }
}
