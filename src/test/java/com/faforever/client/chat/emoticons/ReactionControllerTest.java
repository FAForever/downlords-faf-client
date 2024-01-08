package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonBuilder;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ReactionController;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import java.util.function.Consumer;

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

  @InjectMocks
  private ReactionController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(emoticonService.getImageByShortcode(any())).thenReturn(new Image("http://localhost"));

    loadFxml("theme/chat/emoticons/reaction.fxml", clazz -> instance);
  }

  @Test
  public void testSetEmoticon() {
    Emoticon emoticon = EmoticonBuilder.create()
                                       .defaultValues()
                                       .image(new Image(new ClassPathResource("/images/hydro.png").getPath()))
                                       .get();
    runOnFxThreadAndWait(() -> instance.setReaction(emoticon));
    assertNotNull(instance.emoticonImageView.getImage());
  }

  @Test
  public void testEmoticonClicked() {
    Emoticon emoticon = EmoticonBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setReaction(emoticon);
      instance.setOnReactionClicked(onAction);
      instance.root.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    });

    verify(onAction).accept(emoticon);
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
