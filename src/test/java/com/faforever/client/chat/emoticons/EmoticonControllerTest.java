package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonBuilder;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.test.UITest;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

public class EmoticonControllerTest extends UITest {

  @Mock
  Consumer<String> onAction;

  private EmoticonController instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new EmoticonController();
    loadFxml("theme/chat/emoticons/emoticon.fxml", clazz -> instance);
  }

  @Test
  public void testSetEmoticon() {
    runOnFxThreadAndWait(() -> instance.setEmoticon(EmoticonBuilder.create().defaultValues().get(), onAction));
    assertNotNull(instance.emoticonImageView.getImage());
  }

  @Test
  public void testEmoticonClicked() {
    Emoticon emoticon = EmoticonBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setEmoticon(emoticon, onAction);
      instance.root.fireEvent(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    });
    verify(onAction).accept(emoticon.getShortcodes().get(0));
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
