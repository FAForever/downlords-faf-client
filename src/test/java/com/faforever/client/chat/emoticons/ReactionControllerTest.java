package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonBuilder;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ReactionController;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class ReactionControllerTest extends PlatformTest {

  @Mock
  private EmoticonService emoticonService;
  @Mock
  private ChatService chatService;
  @Mock
  private Consumer<Emoticon> onAction;
  @Mock
  private I18n i18n;

  @InjectMocks
  private ReactionController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(emoticonService.getImageByShortcode(any())).thenReturn(new Image("http://localhost"));
    lenient().when(i18n.number(any()))
             .thenAnswer(invocation -> String.valueOf(invocation.getArgument(0, Number.class)));

    loadFxml("theme/chat/emoticons/reaction.fxml", clazz -> instance);
  }

  @Test
  public void testSetReaction() {
    Emoticon emoticon = EmoticonBuilder.create()
                                       .defaultValues()
                                       .image(new Image(new ClassPathResource("/images/hydro.png").getPath()))
                                       .get();
    runOnFxThreadAndWait(() -> instance.setReaction(emoticon));
    assertNotNull(instance.emoticonImageView.getImage());
  }

  @Test
  public void testReactionClicked() {
    Emoticon emoticon = EmoticonBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setReaction(emoticon);
      instance.setOnReactionClicked(onAction);
      instance.root.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    });

    verify(onAction).accept(emoticon);
  }

  @Test
  public void testSetReactors() {
    ObservableMap<String, String> reactors = FXCollections.observableHashMap();
    runOnFxThreadAndWait(() -> instance.setReactors(reactors));
    assertThat(instance.getRoot().getStyleClass(), not(containsInRelativeOrder("my-reaction")));
    assertThat(instance.label.getText(), equalTo("0"));

    runOnFxThreadAndWait(() -> reactors.put("junit", "1"));
    assertThat(instance.getRoot().getStyleClass(), containsInRelativeOrder("my-reaction"));
    assertThat(instance.label.getText(), equalTo("1"));

    runOnFxThreadAndWait(() -> reactors.put("junit2", "2"));
    assertThat(instance.getRoot().getStyleClass(), containsInRelativeOrder("my-reaction"));
    assertThat(instance.label.getText(), equalTo("2"));

    runOnFxThreadAndWait(() -> reactors.remove("junit"));
    assertThat(instance.getRoot().getStyleClass(), not(containsInRelativeOrder("my-reaction")));
    assertThat(instance.label.getText(), equalTo("1"));
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
