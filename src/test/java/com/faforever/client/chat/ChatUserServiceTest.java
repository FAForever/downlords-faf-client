package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserServiceTest extends AbstractPlainJavaFxTest {

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
  @Mock
  private AvatarBean avatar;

  private Preferences preferences;
  private URL avatarURL;
  private ChatChannelUser chatUser;
  private Clan testClan;

  @Before
  public void setUp() throws Exception {
    chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    preferences = PreferencesBuilder.create().defaultValues()
        .chatPrefs()
        .chatColorMode(ChatColorMode.DEFAULT)
        .then()
        .get();
    avatarURL = new URL("http://avatar.png");
    testClan = new Clan();
    testClan.setTag("testClan");
    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(testClan)));
    when(countryFlagService.loadCountryFlag(anyString())).thenReturn(Optional.of(mock(Image.class)));
    when(uiService.getThemeImage(anyString())).thenReturn(mock(Image.class));
    when(mapService.loadPreview(anyString(), any(PreviewSize.class))).thenReturn(mock(Image.class));
    when(avatarService.loadAvatar(anyString())).thenReturn(mock(Image.class));
    when(avatar.getUrl()).thenReturn(avatarURL);
    when(avatar.getDescription()).thenReturn("fancy");
    when(i18n.getCountryNameLocalized("US")).thenReturn("United States");
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new ChatUserService(
        uiService,
        mapService,
        avatarService,
        clanService,
        countryFlagService,
        preferencesService,
        i18n,
        eventBus
    );
  }

  @Test
  public void testPlayerIsNull() {
    instance.associatePlayerToChatUser(chatUser, null);
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(anyString());
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
    Player player = PlayerBuilder.create("test").defaultValues().get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(anyString());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
    assertNotNull(chatUser.getAvatarChangeListener());
    assertNotNull(chatUser.getSocialStatus());
    assertNotNull(chatUser.getClanTagChangeListener());
    assertNotNull(chatUser.getCountryInvalidationListener());
    assertNotNull(chatUser.getGameStatusChangeListener());
    assertNotNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testListenersRemovedOnSecondAssociation() {
    Player player1 = PlayerBuilder.create("junit1").defaultValues().clan(testClan.getTag()).get();
    instance.associatePlayerToChatUser(chatUser, player1);
    WaitForAsyncUtils.waitForFxEvents();

    ChangeListener<PlayerStatus> gameStatusListener = chatUser.getGameStatusChangeListener();
    ChangeListener<SocialStatus> socialStatusListener = chatUser.getSocialStatusChangeListener();
    ChangeListener<String> clanTagListener = chatUser.getClanTagChangeListener();
    ChangeListener<String> avatarListener = chatUser.getAvatarChangeListener();
    ChangeListener<String> countryListener = chatUser.getCountryInvalidationListener();
    ChangeListener<Boolean> displayedListener = chatUser.getDisplayedChangeListener();

    Player player2 = PlayerBuilder.create("junit2").defaultValues().clan(testClan.getTag()).get();
    instance.associatePlayerToChatUser(chatUser, player2);
    WaitForAsyncUtils.waitForFxEvents();

    assertNotEquals(gameStatusListener, chatUser.getGameStatusChangeListener());
    assertNotEquals(socialStatusListener, chatUser.getSocialStatusChangeListener());
    assertNotEquals(clanTagListener, chatUser.getClanTagChangeListener());
    assertNotEquals(avatarListener, chatUser.getAvatarChangeListener());
    assertNotEquals(countryListener, chatUser.getCountryInvalidationListener());
    assertNotEquals(displayedListener, chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testListenersRemovedOnNullAssociation() {
    Player player1 = PlayerBuilder.create("junit1").defaultValues().clan(testClan.getTag()).get();
    instance.associatePlayerToChatUser(chatUser, player1);
    WaitForAsyncUtils.waitForFxEvents();

    assertNotNull(chatUser.getGameStatusChangeListener());
    assertNotNull(chatUser.getSocialStatusChangeListener());
    assertNotNull(chatUser.getClanTagChangeListener());
    assertNotNull(chatUser.getAvatarChangeListener());
    assertNotNull(chatUser.getCountryInvalidationListener());
    assertNotNull(chatUser.getDisplayedChangeListener());

    instance.associatePlayerToChatUser(chatUser, null);
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(chatUser.getGameStatusChangeListener());
    assertNull(chatUser.getSocialStatusChangeListener());
    assertNull(chatUser.getClanTagChangeListener());
    assertNull(chatUser.getAvatarChangeListener());
    assertNull(chatUser.getCountryInvalidationListener());
    assertNull(chatUser.getDisplayedChangeListener());
  }

  @Test
  public void testClanNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().clan(testClan.getTag()).get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService).getClanByTag(testClan.getTag());
    assertEquals(chatUser.getClan().orElse(null), testClan);
    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().clan(null).get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    assertTrue(chatUser.getClan().isEmpty());
    assertTrue(chatUser.getClanTag().isEmpty());
  }

  @Test
  public void testAvatarNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().avatar(avatar).get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(avatarService).loadAvatar(avatarURL.toExternalForm());
    assertTrue(chatUser.getAvatar().isPresent());
  }

  @Test
  public void testAvatarNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(avatarService, never()).loadAvatar(anyString());
    assertTrue(chatUser.getAvatar().isEmpty());
  }

  @Test
  public void testCountryNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().country("US").get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(countryFlagService).loadCountryFlag("US");
    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("United States", chatUser.getCountryName().orElse(null));
  }

  @Test
  public void testCountryNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().country(null).get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(countryFlagService).loadCountryFlag(null);
    assertTrue(chatUser.getCountryFlag().isEmpty());
    assertTrue(chatUser.getCountryName().isEmpty());
  }

  @Test
  public void testStatusToIdle() {
    Player player = PlayerBuilder.create("junit").defaultValues().game(null).get();
    when(i18n.get("game.gameStatus.none")).thenReturn("None");
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService, never()).getThemeImage(anyString());
    assertFalse(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.IDLE, chatUser.getGameStatus().orElse(null));
    assertEquals("None", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToPlaying() {
    Game game = GameBuilder.create().defaultValues().status(GameStatus.PLAYING).get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    when(i18n.get("game.gameStatus.playing")).thenReturn("Playing");
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.PLAYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Playing", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToHosting() {
    Game game = GameBuilder.create().defaultValues().status(GameStatus.OPEN).host("junit").get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    when(i18n.get("game.gameStatus.hosting")).thenReturn("Hosting");
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.HOSTING, chatUser.getGameStatus().orElse(null));
    assertEquals("Hosting", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testStatusToLobbying() {
    Game game = GameBuilder.create().defaultValues().status(GameStatus.OPEN).get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    when(i18n.get("game.gameStatus.lobby")).thenReturn("Waiting for game to start");
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.LOBBYING, chatUser.getGameStatus().orElse(null));
    assertEquals("Waiting for game to start", chatUser.getStatusTooltipText().orElse(null));
  }

  @Test
  public void testUserColorNotSet() {
    instance.populateColor(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatUser.getColor().isEmpty());
  }

  @Test
  public void testUserColorSetNoPlayer() {
    preferences.getChat().setUserToColor(FXCollections.observableMap(Map.of("junit", Color.AQUA)));
    instance.populateColor(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testGroupColorSet() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.AQUA)));
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .socialStatus(SocialStatus.FRIEND)
        .get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.AQUA);
  }

  @Test
  public void testModeratorColorOverGroup() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.MODERATOR, Color.RED, ChatUserCategory.FRIEND, Color.AQUA)));
    chatUser.setModerator(true);
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .socialStatus(SocialStatus.FRIEND)
        .get();
    instance.associatePlayerToChatUser(chatUser, player);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatUser.getColor().isPresent());
    assertEquals(chatUser.getColor().get(), Color.RED);
  }
}
