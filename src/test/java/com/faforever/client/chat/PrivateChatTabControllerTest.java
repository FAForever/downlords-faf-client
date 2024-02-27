package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends PlatformTest {

  @Mock
  private TimeService timeService;
  @Mock
  private AudioService audioService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private ThemeService themeService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private ReportingService reportingService;
  @Mock
  private NavigationHandler navigationHandler;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PrivatePlayerInfoController privatePlayerInfoController;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private ChatService chatService;
  @Spy
  private ChatPrefs chatPrefs;

  @Mock
  private ChatMessageViewController chatMessageViewController;
  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  @InjectMocks
  private PrivateChatTabController instance;

  private String playerName;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().get();
    playerName = player.getUsername();

    lenient().when(chatMessageViewController.chatChannelProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(chatService.getCurrentUsername()).thenReturn(playerName);
    lenient().when(themeService.getThemeFileUrl(any())).then(invocation -> getThemeFileUrl(invocation.getArgument(0)));
    lenient().when(privatePlayerInfoController.chatUserProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(avatarService.loadAvatar(player.getAvatar())).thenReturn(new Image(InputStream.nullInputStream()));

    ChatChannel chatChannel = new ChatChannel(playerName);
    ChatChannelUser chatChannelUser = new ChatChannelUser(playerName, new ChatChannel(playerName));
    chatChannelUser.setPlayer(player);
    chatChannel.addUser(chatChannelUser);

    loadFxml("theme/chat/private_chat_tab.fxml", clazz -> {
      if (clazz == PrivatePlayerInfoController.class) {
        return privatePlayerInfoController;
      }
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      if (clazz == ChatMessageViewController.class) {
        return chatMessageViewController;
      }
      if (clazz == EmoticonsWindowController.class) {
        return emoticonsWindowController;
      }
      return instance;
    });

    runOnFxThreadAndWait(() -> {
      TabPane tabPane = new TabPane();
      tabPane.setSkin(new TabPaneSkin(tabPane));
      tabPane.getTabs().add(instance.getRoot());
      instance.setChatChannel(chatChannel);
    });
  }

  @Test
  public void checkSetAvatarToTabIfPlayerHasAvatar() {
    assertTrue(instance.avatarImageView.isVisible());
    assertNotNull(instance.avatarImageView.getImage());
    assertFalse(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkSetDefaultIconForTabIfPlayerHasNoAvatar() {
    player.setAvatar(null);
    assertFalse(instance.avatarImageView.isVisible());
    assertNull(instance.avatarImageView.getImage());
    assertTrue(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkPlayerAvatarListener() throws Exception {
    assertNotNull(instance.avatarImageView.getImage());

    Image newAvatarImage = new Image(InputStream.nullInputStream());
    AvatarBean avatarBean = Instancio.create(AvatarBean.class);
    when(avatarService.loadAvatar(avatarBean)).thenReturn(newAvatarImage);
    runOnFxThreadAndWait(() -> player.setAvatar(avatarBean));
    assertEquals(newAvatarImage, instance.avatarImageView.getImage());
  }
}
