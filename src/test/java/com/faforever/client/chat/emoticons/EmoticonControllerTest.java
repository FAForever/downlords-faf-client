package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonBuilder;
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

public class EmoticonControllerTest extends PlatformTest {

  @Mock
  private EmoticonService emoticonService;
  @Mock
  private Consumer<String> onAction;

  @InjectMocks
  private EmoticonController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(emoticonService.getImageByShortcode(any())).thenReturn(new Image("http://localhost"));

    loadFxml("theme/chat/emoticons/emoticon.fxml", clazz -> instance);
  }

  @Test
  public void testSetEmoticon() {
    Emoticon emoticon = EmoticonBuilder.create()
        .defaultValues()
        .image(new Image(new ClassPathResource("/images/hydro.png").getPath()))
        .get();
    runOnFxThreadAndWait(() -> instance.setEmoticon(emoticon, onAction));
    assertNotNull(instance.emoticonImageView.getImage());
  }

  @Test
  public void testEmoticonClicked() {
    Emoticon emoticon = EmoticonBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setEmoticon(emoticon, onAction);
      instance.root.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    });
    verify(onAction).accept(emoticon.shortcodes().getFirst());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
