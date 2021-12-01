package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class EmoticonsWindowControllerTest extends UITest {

  @Mock
  private EmoticonService emoticonService;
  @Mock
  private UiService uiService;
  @Mock
  private EmoticonsGroupController emoticonsGroupController;

  private TextInputControl textField;
  private List<EmoticonsGroup> emoticonsGroups;

  private EmoticonsWindowController instance;

  @BeforeEach
  public void setUp() throws Exception {
    emoticonsGroups = List.of(
        EmoticonGroupBuilder.create().defaultValues().get(),
        EmoticonGroupBuilder.create().defaultValues().get()
    );
    when(emoticonService.getEmoticonsGroups()).thenReturn(emoticonsGroups);
    when(uiService.loadFxml("theme/chat/emoticons/emoticons_group.fxml")).thenReturn(emoticonsGroupController);
    when(emoticonsGroupController.getRoot()).thenReturn(new VBox(), new VBox()); // Root do not allow to put the same views

    instance = new EmoticonsWindowController(emoticonService, uiService);
    textField = new TextField();
    instance.associateWith(textField);
    loadFxml("theme/chat/emoticons/emoticons_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetEmoticonsGroupViews() {
    assertEquals(2, instance.root.getChildren().size());
  }

  @Test
  public void testOnEmoticonClicked() {
    String shortcode = emoticonsGroups.get(0).getEmoticons().get(0).getShortcodes().get(0);
    runOnFxThreadAndWait(() -> instance.onEmoticonClicked().accept(shortcode));

    assertEquals(" ".concat(shortcode).concat(" "), textField.getText());
    assertEquals(" ".concat(shortcode).concat(" ").length(), textField.getCaretPosition());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}
