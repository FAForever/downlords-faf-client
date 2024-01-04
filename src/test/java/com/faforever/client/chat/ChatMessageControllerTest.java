package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

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

  @InjectMocks
  private ChatMessageController instance;

  private final ChatChannelUser user = new ChatChannelUser("junit", new ChatChannel("#testChannel"));
  private final PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
  private final Image image = new Image("http://localhost");


  @BeforeEach
  public void setup() throws Exception {
    lenient().when(avatarService.loadAvatar(any())).thenReturn(image);
    lenient().when(countryFlagService.loadCountryFlag(any())).thenReturn(Optional.empty());
    lenient().when(timeService.asShortTime(any())).thenReturn("12:00");
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(chatService.getMentionPattern())
             .thenReturn(Pattern.compile("(^|[^A-Za-z0-9-])junit([^A-Za-z0-9-]|$)"));
    lenient().when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile(":\\)"));
    lenient().when(emoticonService.getImageByShortcode(any())).thenReturn(image);
    lenient().when(timeService.asShortTime(any())).thenReturn("now");
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenReturn(new SimpleObjectProperty<>(image));

    user.setPlayer(player);
    user.setColor(new Color(0, 0, 0, 0));

    loadFxml("theme/chat/chat_message.fxml", clazz -> instance);
  }

  @Test
  public void testClickAuthor() {
    instance.setChatMessage(new ChatMessage(Instant.now(), user, "", ""));
    runOnFxThreadAndWait(() -> instance.authorLabel.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 2)));
    verify(chatService).onInitiatePrivateChat("junit");
  }

  @Test
  public void testMultipleWords() {
    instance.setChatMessage(new ChatMessage(Instant.now(), user, "Hello world!", ""));

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
    instance.setChatMessage(new ChatMessage(Instant.now(), user, "#test", ""));

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
    instance.setChatMessage(new ChatMessage(Instant.now(), user, "https://www.google.com", ""));

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
  public void testSelf() {
    instance.setChatMessage(new ChatMessage(Instant.now(), user, "junit", ""));

    ObservableList<Node> children = instance.message.getChildren();
    assertThat(children, hasSize(1));

    Node first = children.getFirst();
    assertThat(first, instanceOf(Text.class));
    Text text = (Text) first;
    assertThat(text.getText(), equalTo("junit "));

    assertThat(text.getStyleClass(), contains("self"));
  }

  @Test
  public void testEmoticon() {
    instance.setChatMessage(new ChatMessage(Instant.now(), user, ":)", ""));

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
}
