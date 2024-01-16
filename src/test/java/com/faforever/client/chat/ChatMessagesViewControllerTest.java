package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatMessagesViewControllerTest extends PlatformTest {

  @Mock
  private ChatService chatService;
  @Mock
  private TimeService timeService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ThemeService themeService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private I18n i18n;
  @Spy
  private ChatPrefs chatPrefs;

  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  @InjectMocks
  private ChatMessageViewController instance;

  private final ChatChannel chatChannel = new ChatChannel("#testChannel");
  private final ChatChannelUser user = new ChatChannelUser("junit", chatChannel);


  @BeforeEach
  public void setup() throws Exception {
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(timeService.asShortTime(any())).thenReturn("now");
    lenient().when(emoticonsWindowController.getRoot()).thenReturn(new VBox());
    lenient().when(chatService.getMentionPattern())
             .thenReturn(Pattern.compile("(^|[^A-Za-z0-9-])" + Pattern.quote(user.getUsername()) + "([^A-Za-z0-9-]|$)",
                                         CASE_INSENSITIVE));

    Stage stage = mock(Stage.class);
    lenient().when(stage.focusedProperty()).thenReturn(new SimpleBooleanProperty());

    StageHolder.setStage(stage);


    loadFxml("theme/chat/chat_message_view.fxml", clazz -> {
      if (clazz == EmoticonsWindowController.class) {
        return emoticonsWindowController;
      }
      return instance;
    });

    instance.setChatChannel(chatChannel);
  }

  @Test
  public void testUpdateTypingState() {
    instance.messageTextField.appendText("a");
    verify(chatService).setActiveTypingState(any());

    instance.messageTextField.deleteText(0, 1);
    verify(chatService).setDoneTypingState(any());
  }

  @Test
  public void testUpdateTypingStateOnSend() {
    instance.messageTextField.appendText("a");
    verify(chatService).setActiveTypingState(any());

    when(chatService.sendMessageInBackground(any(), any())).thenReturn(completedFuture(null));

    fxApplicationThreadExecutor.executeAndWait(() -> instance.onSendMessage());
    verify(chatService, never()).setDoneTypingState(any());
  }

  @Test
  public void testTypingLabel() {
    assertFalse(instance.typingLabel.isVisible());

    runOnFxThreadAndWait(() -> chatChannel.createUserIfNecessary("test1", chatUser -> chatUser.setTyping(true)));

    verify(i18n).get("chat.typing.single", "test1");
    assertTrue(instance.typingLabel.isVisible());

    runOnFxThreadAndWait(() -> chatChannel.createUserIfNecessary("test2", chatUser -> chatUser.setTyping(true)));
    verify(i18n).get("chat.typing.double", "test1", "test2");
    assertTrue(instance.typingLabel.isVisible());

    runOnFxThreadAndWait(() -> chatChannel.createUserIfNecessary("test3", chatUser -> chatUser.setTyping(true)));
    verify(i18n).get("chat.typing.many");
    assertTrue(instance.typingLabel.isVisible());
  }

  @Test
  public void testOnSendMessageSuccessful() {
    String message = "Some message";
    instance.messageTextField.setText(message);
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(eq(chatChannel), eq(message));
    assertThat(instance.messageTextField.getText(), is(emptyString()));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() {
    String message = "Some message";
    instance.messageTextField.setText(message);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(chatChannel, message);
    assertThat(instance.messageTextField.getText(), is(""));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testOnEmoticonClicked() {
    ArgumentCaptor<Consumer<Emoticon>> captor = captor();

    verify(emoticonsWindowController).setOnEmoticonClicked(captor.capture());

    runOnFxThreadAndWait(() -> captor.getValue().accept(new Emoticon(List.of(":)"), "")));

    String expected = " " + ":)" + " ";
    assertEquals(" :) ", instance.messageTextField.getText());
    assertEquals(expected.length(), instance.messageTextField.getCaretPosition());
  }
}
