package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.AnchorPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class EmoticonsGroupControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private PlatformService platformService;
  @Mock
  private EmoticonController emoticonController;

  @InjectMocks
  private EmoticonsGroupController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(uiService.loadFxml("theme/chat/emoticons/emoticon.fxml")).thenReturn(emoticonController);
    lenient().when(emoticonController.getRoot())
        .thenReturn(new AnchorPane(), new AnchorPane()); // FlowPane does not allow to put the same views
    lenient().when(emoticonController.onEmoticonClickedProperty()).thenReturn(new SimpleObjectProperty<>());

    loadFxml("theme/chat/emoticons/emoticons_group.fxml", clazz -> instance);
  }

  @Test
  public void testSetGroup() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().attribution("").get();
    runOnFxThreadAndWait(() -> instance.setEmoticonsGroup(emoticonsGroup));

    assertEquals(emoticonsGroup.name(), instance.groupLabel.getText());
    assertFalse(instance.attributionPane.isVisible());
    assertEquals(2, instance.emoticonsPane.getChildren().size());
  }

  @Test
  public void testSetGroupWithAttribution() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> instance.setEmoticonsGroup(emoticonsGroup));

    assertEquals(emoticonsGroup.name(), instance.groupLabel.getText());
    assertEquals(emoticonsGroup.attributionUrl(), instance.attributionHyperlink.getText());
    assertTrue(instance.attributionPane.isVisible());
    assertEquals(2, instance.emoticonsPane.getChildren().size());
  }

  @Test
  public void testOnClickAttribution() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setEmoticonsGroup(emoticonsGroup);
      instance.attributionHyperlink.fire();
    });
    verify(platformService).showDocument(emoticonsGroup.attributionUrl());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
