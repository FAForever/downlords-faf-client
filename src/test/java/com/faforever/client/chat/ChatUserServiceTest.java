package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatUserGameChangeEvent;
import com.faforever.client.chat.event.ChatUserPopulateEvent;
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
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
  private URL avatarURL;

  private Clan testClan;

  @Before
  public void setUp() throws Exception {
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

    instance = new ChatUserService(
        uiService,
        mapService,
        avatarService,
        clanService,
        countryFlagService,
        i18n,
        eventBus
    );
  }

  @Test
  public void testPlayerIsNull() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(anyString());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
  }

  @Test
  public void testChatUserNotDisplayed() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .displayed(false)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    instance.onChatUserGameChange(new ChatUserGameChangeEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(anyString());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
  }

  @Test
  public void testChatUserPopulated() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .populated(true)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    verify(countryFlagService, never()).loadCountryFlag(anyString());
    verify(avatarService, never()).loadAvatar(anyString());
    verify(mapService, never()).loadPreview(anyString(), any(PreviewSize.class));
    verify(uiService, never()).getThemeImage(anyString());
  }

  @Test
  public void testClanNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().clan(testClan.getTag()).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService).getClanByTag(testClan.getTag());
    assertEquals(chatUser.getClan().orElse(null), testClan);
    assertEquals(chatUser.getClanTag().orElse(null), String.format("[%s]", testClan.getTag()));
  }

  @Test
  public void testClanNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().clan(null).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(clanService, never()).getClanByTag(anyString());
    assertTrue(chatUser.getClan().isEmpty());
    assertTrue(chatUser.getClanTag().isEmpty());
  }

  @Test
  public void testAvatarNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().avatar(avatar).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(avatarService).loadAvatar(avatarURL.toExternalForm());
    assertTrue(chatUser.getAvatar().isPresent());
  }

  @Test
  public void testAvatarNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(avatarService, never()).loadAvatar(anyString());
    assertTrue(chatUser.getAvatar().isEmpty());
  }

  @Test
  public void testCountryNotNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().country("US").get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(countryFlagService).loadCountryFlag("US");
    assertTrue(chatUser.getCountryFlag().isPresent());
    assertEquals("United States", chatUser.getCountryName().orElse(null));
  }

  @Test
  public void testCountryNull() {
    Player player = PlayerBuilder.create("junit").defaultValues().country(null).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserPopulate(new ChatUserPopulateEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(countryFlagService).loadCountryFlag(null);
    assertTrue(chatUser.getCountryFlag().isEmpty());
    assertTrue(chatUser.getCountryName().isEmpty());
  }

  @Test
  public void testStatusToIdle() {
    Player player = PlayerBuilder.create("junit").defaultValues().game(null).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserGameChange(new ChatUserGameChangeEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService, never()).getThemeImage(anyString());
    assertFalse(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.IDLE, chatUser.getStatus().orElse(null));
  }

  @Test
  public void testStatusToPlaying() {
    Game game = GameBuilder.create().defaultValues().state(GameStatus.PLAYING).get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserGameChange(new ChatUserGameChangeEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.PLAYING, chatUser.getStatus().orElse(null));
  }

  @Test
  public void testStatusToHosting() {
    Game game = GameBuilder.create().defaultValues().state(GameStatus.OPEN).host("junit").get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserGameChange(new ChatUserGameChangeEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.HOSTING, chatUser.getStatus().orElse(null));
  }

  @Test
  public void testStatusToLobbying() {
    Game game = GameBuilder.create().defaultValues().state(GameStatus.OPEN).get();
    Player player = PlayerBuilder.create("junit").defaultValues().game(game).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.onChatUserGameChange(new ChatUserGameChangeEvent(chatUser));
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
    assertTrue(chatUser.getMapImage().isPresent());
    assertEquals(PlayerStatus.LOBBYING, chatUser.getStatus().orElse(null));
  }
}
