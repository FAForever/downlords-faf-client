package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.chat.emoticons.Reaction;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatMessageControllerTest extends PlatformTest {

  @Mock
  private AvatarService avatarService;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private TimeService timeService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ChatService chatService;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @InjectMocks
  private ChatMessageController instance;

  private final ChatChannelUser user = new ChatChannelUser("junit", new ChatChannel("#testChannel"));
  private final PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
  private Image image;


  @BeforeEach
  public void setup() throws Exception {
    image = new Image("http://localhost");
    lenient().when(avatarService.loadAvatar(any())).thenReturn(image);
    lenient().when(countryFlagService.loadCountryFlag(any())).thenReturn(Optional.empty());
    lenient().when(timeService.asShortTime(any())).thenReturn("12:00");
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(chatService.getMentionPattern())
             .thenReturn(Pattern.compile("(^|[^A-Za-z0-9-])junit([^A-Za-z0-9-]|$)"));
    lenient().when(timeService.asShortTime(any())).thenReturn("now");
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenReturn(new SimpleObjectProperty<>(image));
    lenient().when(i18n.get("pending")).thenReturn("pending");

    user.setPlayer(player);
    user.setColor(new Color(0, 0, 0, 0));

    loadFxml("theme/chat/chat_message.fxml", clazz -> instance);
  }

  @Test
  public void testPending() {
    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "", Type.PENDING, null)));
    assertThat(instance.timeLabel.getText(), equalTo("pending"));

    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "", Type.MESSAGE, null)));
    assertThat(instance.timeLabel.getText(), not(equalTo("pending")));
  }

  @Test
  public void testClickAuthor() {
    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "", Type.MESSAGE, null)));
    runOnFxThreadAndWait(() -> instance.authorLabel.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 2)));
    verify(chatService).joinPrivateChat("junit");
  }

  @Test
  public void testMultipleWords() {
    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "Hello world!", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(2));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Text.class));
    Text firstText = (Text) first;
    assertThat(firstText.getText(), equalTo("Hello "));
    assertThat(firstText.getStyle(), containsString("-fx-fill: #000000"));

    Node last = children.getLast();
    assertThat(last, instanceOf(Text.class));
    Text lastText = (Text) last;
    assertThat(lastText.getText(), equalTo("world! "));
    assertThat(lastText.getStyle(), containsString("-fx-fill: #000000"));
  }

  @Test
  public void testChannel() {
    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "#test", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Hyperlink.class));
    Hyperlink hyperlink = (Hyperlink) first;
    assertThat(hyperlink.getText(), equalTo("#test "));

    hyperlink.getOnAction().handle(null);
    verify(chatService).joinChannel("#test");
  }

  @Test
  public void testUrl() {
    runOnFxThreadAndWait(() -> instance.setChatMessage(
        new ChatMessage(null, Instant.now(), user, "https://www.google.com", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Hyperlink.class));
    Hyperlink hyperlink = (Hyperlink) first;
    assertThat(hyperlink.getText(), equalTo("https://www.google.com "));

    hyperlink.getOnAction().handle(null);
    verify(platformService).showDocument("https://www.google.com");
  }

  @Test
  public void testNoSchemeUrl() {
    runOnFxThreadAndWait(() -> instance.setChatMessage(
        new ChatMessage(null, Instant.now(), user, "www.google.com", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Hyperlink.class));
    Hyperlink hyperlink = (Hyperlink) first;
    assertThat(hyperlink.getText(), equalTo("www.google.com "));

    hyperlink.getOnAction().handle(null);
    verify(platformService).showDocument("https://www.google.com");
  }

  @Test
  public void testSelf() {
    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, "junit", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Text.class));
    Text text = (Text) first;
    assertThat(text.getText(), equalTo("junit "));

    assertThat(text.getStyle(), containsString("-fx-fill: #FFA500"));
  }

  @Test
  public void testEmoticon() {
    when(emoticonService.isEmoticonShortcode(any())).thenReturn(true);
    when(emoticonService.getImageByShortcode(any())).thenReturn(image);

    runOnFxThreadAndWait(
        () -> instance.setChatMessage(new ChatMessage(null, Instant.now(), user, ":)", Type.MESSAGE, null)));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Pane.class));
    Pane pane = (Pane) first;

    assertThat(pane.getPadding(), equalTo(new Insets(0, 5, 0, 0)));

    Node paneChild = pane.getChildren().getFirst();
    assertThat(paneChild, instanceOf(ImageView.class));
    ImageView imageView = (ImageView) paneChild;

    assertThat(imageView.getFitHeight(), equalTo(24d));
    assertThat(imageView.getFitWidth(), equalTo(24d));

    verify(emoticonService).getImageByShortcode(":)");
  }

  @Test
  public void testReactionChange() {
    ReactionController mockedReactionController = mock(ReactionController.class);
    when(uiService.loadFxml("theme/chat/emoticons/reaction.fxml")).thenReturn(mockedReactionController);
    when(mockedReactionController.getRoot()).thenReturn(new HBox());
    when(mockedReactionController.onReactionClickedProperty()).thenReturn(new SimpleObjectProperty<>());

    Emoticon emoticon = new Emoticon(List.of(), "");
    ChatMessage message = new ChatMessage("2", Instant.now(), user, "hello", Type.MESSAGE, null);
    runOnFxThreadAndWait(() -> instance.setChatMessage(message));

    assertThat(instance.reactionsContainer.isVisible(), is(false));
    assertThat(instance.reactionsContainer.getChildren(), empty());

    runOnFxThreadAndWait(() -> message.addReaction(new Reaction("1", "2", emoticon, "junit")));

    assertThat(instance.reactionsContainer.isVisible(), is(true));
    assertThat(instance.reactionsContainer.getChildren(), hasSize(1));
  }

  @Test
  public void testOnReact() {
    EmoticonsWindowController mockedEmoticonsWindowController = mock(EmoticonsWindowController.class);
    when(uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml")).thenReturn(mockedEmoticonsWindowController);
    when(mockedEmoticonsWindowController.getRoot()).thenReturn(new VBox());

    ChatMessage message = new ChatMessage(null, Instant.now(), user, "hello", Type.MESSAGE, null);
    runOnFxThreadAndWait(() -> instance.setChatMessage(message));
    instance.onReactButtonClicked();

    ArgumentCaptor<Consumer<Emoticon>> captor = captor();

    verify(mockedEmoticonsWindowController).setOnEmoticonClicked(captor.capture());

    Consumer<Emoticon> consumer = captor.getValue();
    Emoticon emoticon = new Emoticon(List.of(), "");
    runOnFxThreadAndWait(() -> consumer.accept(emoticon));

    verify(chatService).reactToMessageInBackground(message, emoticon);
  }

  @Test
  public void testOnReactAlreadyReacted() {
    EmoticonsWindowController mockedEmoticonsWindowController = mock(EmoticonsWindowController.class);
    when(uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml")).thenReturn(mockedEmoticonsWindowController);
    when(mockedEmoticonsWindowController.getRoot()).thenReturn(new VBox());

    Emoticon emoticon = new Emoticon(List.of(), "");
    ChatMessage message = new ChatMessage(null, Instant.now(), user, "hello", Type.MESSAGE, null);
    message.addReaction(new Reaction("1", "2", emoticon, "junit"));
    runOnFxThreadAndWait(() -> instance.setChatMessage(message));
    instance.onReactButtonClicked();

    ArgumentCaptor<Consumer<Emoticon>> captor = captor();

    verify(mockedEmoticonsWindowController).setOnEmoticonClicked(captor.capture());

    Consumer<Emoticon> consumer = captor.getValue();
    runOnFxThreadAndWait(() -> consumer.accept(emoticon));

    verify(chatService, never()).reactToMessageInBackground(message, emoticon);
  }

  @Test
  public void testButtonReact() {
    ReactionController mockedReactionController = mock(ReactionController.class);
    when(uiService.loadFxml("theme/chat/emoticons/reaction.fxml")).thenReturn(mockedReactionController);
    when(mockedReactionController.getRoot()).thenReturn(new HBox());
    SimpleObjectProperty<Consumer<Emoticon>> consumerProperty = new SimpleObjectProperty<>();
    when(mockedReactionController.onReactionClickedProperty()).thenReturn(consumerProperty);

    Emoticon emoticon = new Emoticon(List.of(), "");
    ChatMessage message = new ChatMessage(null, Instant.now(), user, "hello", Type.MESSAGE, null);
    message.addReaction(new Reaction("1", "2", emoticon, "other"));
    runOnFxThreadAndWait(() -> instance.setChatMessage(message));

    Consumer<Emoticon> consumer = consumerProperty.getValue();
    runOnFxThreadAndWait(() -> consumer.accept(emoticon));

    verify(chatService).reactToMessageInBackground(message, emoticon);
  }

  @Test
  public void testButtonReactAlreadyReacted() {
    ReactionController mockedReactionController = mock(ReactionController.class);
    when(uiService.loadFxml("theme/chat/emoticons/reaction.fxml")).thenReturn(mockedReactionController);
    when(mockedReactionController.getRoot()).thenReturn(new HBox());
    SimpleObjectProperty<Consumer<Emoticon>> consumerProperty = new SimpleObjectProperty<>();
    when(mockedReactionController.onReactionClickedProperty()).thenReturn(consumerProperty);

    Emoticon emoticon = new Emoticon(List.of(), "");
    ChatMessage message = new ChatMessage("2", Instant.now(), user, "hello", Type.MESSAGE, null);
    message.addReaction(new Reaction("1", "2", emoticon, "junit"));
    runOnFxThreadAndWait(() -> instance.setChatMessage(message));

    Consumer<Emoticon> consumer = consumerProperty.getValue();
    runOnFxThreadAndWait(() -> consumer.accept(emoticon));

    verify(chatService, never()).reactToMessageInBackground(message, emoticon);
    verify(chatService).redactMessageInBackground(user.getChannel(), "1");
  }

  @Test
  public void testMessageWithReply() {
    ChatMessage targetMessage = new ChatMessage(null, Instant.now().minusSeconds(30), user, "boo", Type.MESSAGE, null);
    ChatMessage message = new ChatMessage(null, Instant.now(), user, "hello", Type.MESSAGE, targetMessage);

    Consumer<ChatMessage> mockConsumer = mock();

    runOnFxThreadAndWait(() -> instance.setChatMessage(message));
    instance.setOnReplyClicked(mockConsumer);

    instance.replyContainer.getOnMouseClicked().handle(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    assertThat(instance.replyContainer.isVisible(), equalTo(true));
    verify(mockConsumer).accept(targetMessage);
  }

  @Test
  public void testReplyButtonClicked() {
    ChatMessage message = new ChatMessage(null, Instant.now(), user, "hello", Type.MESSAGE, null);

    Consumer<ChatMessage> mockConsumer = mock();

    runOnFxThreadAndWait(() -> instance.setChatMessage(message));
    instance.setOnReplyButtonClicked(mockConsumer);

    instance.replyButton.getOnAction().handle(null);

    verify(mockConsumer).accept(message);
  }
}
