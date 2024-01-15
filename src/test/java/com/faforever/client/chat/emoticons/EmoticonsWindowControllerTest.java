package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class EmoticonsWindowControllerTest extends PlatformTest {

  @Mock
  private EmoticonService emoticonService;
  @Mock
  private UiService uiService;
  @Mock
  private EmoticonsGroupController emoticonsGroupController;

  @InjectMocks
  private EmoticonsWindowController instance;

  @BeforeEach
  public void setUp() throws Exception {
    List<EmoticonsGroup> emoticonsGroups = List.of(EmoticonGroupBuilder.create().defaultValues().get(),
                                                   EmoticonGroupBuilder.create().defaultValues().get());
    when(emoticonService.getEmoticonsGroups()).thenReturn(emoticonsGroups);
    when(uiService.loadFxml("theme/chat/emoticons/emoticons_group.fxml")).thenReturn(emoticonsGroupController);
    when(emoticonsGroupController.getRoot()).thenReturn(new VBox(), new VBox()); // Root do not allow to put the same views

    lenient().when(emoticonsGroupController.onEmoticonClickedProperty()).thenReturn(new SimpleObjectProperty<>());

    loadFxml("theme/chat/emoticons/emoticons_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetEmoticonsGroupViews() {
    assertEquals(2, instance.root.getChildren().size());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
