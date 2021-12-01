package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.AnchorPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmoticonsGroupControllerTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private PlatformService platformService;
  @Mock
  EmoticonController emoticonController;

  private EmoticonsGroupController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/chat/emoticons/emoticon.fxml")).thenReturn(emoticonController);
    when(emoticonController.getRoot()).thenReturn(new AnchorPane(), new AnchorPane()); // FlowPane does not allow to put the same views

    instance = new EmoticonsGroupController(uiService, platformService);
    loadFxml("theme/chat/emoticons/emoticons_group.fxml", clazz -> instance);
  }

  @Test
  public void testSetGroup() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().attribution("").get();
    runOnFxThreadAndWait(() -> instance.setGroup(emoticonsGroup, any()));

    assertEquals(emoticonsGroup.getName(), instance.groupLabel.getText());
    assertFalse(instance.attributionPane.isVisible());
    assertEquals(2, instance.emoticonsPane.getChildren().size());
  }

  @Test
  public void testSetGroupWithAttribution() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> instance.setGroup(emoticonsGroup, any()));

    assertEquals(emoticonsGroup.getName(), instance.groupLabel.getText());
    assertEquals(emoticonsGroup.getAttribution(), instance.attributionHyperlink.getText());
    assertTrue(instance.attributionPane.isVisible());
    assertEquals(2, instance.emoticonsPane.getChildren().size());
  }

  @Test
  public void testOnClickAttribution() {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().get();
    runOnFxThreadAndWait(() -> {
      instance.setGroup(emoticonsGroup, any());
      instance.attributionHyperlink.fire();
    });
    verify(platformService).showDocument(emoticonsGroup.getAttribution());
  }
}
