package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatCategoryColorPickerCustomMenuItemControllerTest extends PlatformTest {


  @Spy
  private ChatPrefs chatPrefs;

  private ChatCategoryColorPickerCustomMenuItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatPrefs.setChatColorMode(ChatColorMode.DEFAULT);

    instance = new ChatCategoryColorPickerCustomMenuItemController(chatPrefs);

    loadFxml("theme/chat/color_picker_menu_item.fxml", clazz -> instance, instance);
  }

  @Test
  public void testSetCurrentValue() {
    chatPrefs.setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.BLACK)));
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(ChatUserCategory.FRIEND);
    });
    assertEquals(Color.BLACK, instance.colorPicker.getValue());
    assertTrue(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testChangeColorToAnother() {
    Map<ChatUserCategory, Color> colorMap = new HashMap<>();
    colorMap.put(ChatUserCategory.FRIEND, Color.BLACK);
    chatPrefs.setGroupToColor(FXCollections.observableMap(colorMap));
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(ChatUserCategory.FRIEND);
      instance.colorPicker.setValue(Color.WHITE);
    });
    assertEquals(Color.WHITE, chatPrefs.getGroupToColor().get(ChatUserCategory.FRIEND));
    assertTrue(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testClearColor() {
    Map<ChatUserCategory, Color> colorMap = new HashMap<>();
    colorMap.put(ChatUserCategory.FRIEND, Color.BLACK);
    chatPrefs.setGroupToColor(FXCollections.observableMap(colorMap));
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(ChatUserCategory.FRIEND);
      instance.colorPicker.setValue(null);
    });
    assertNull(chatPrefs.getGroupToColor().get(ChatUserCategory.FRIEND));
    assertFalse(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testVisibleItem() {
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(ChatUserCategory.FRIEND);
    });
    assertTrue(instance.getRoot().isVisible());
  }

  @Test
  public void testInvisibleItem() {
    chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(ChatUserCategory.FRIEND);
    });
    assertFalse(instance.getRoot().isVisible());
  }
}
