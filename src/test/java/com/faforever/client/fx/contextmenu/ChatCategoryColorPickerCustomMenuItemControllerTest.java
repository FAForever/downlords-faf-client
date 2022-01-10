package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ChatCategoryColorPickerCustomMenuItemControllerTest extends UITest {

  @Mock
  private PreferencesService preferencesService;

  private Preferences preferences;
  private ChatCategoryColorPickerCustomMenuItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().chatPrefs()
        .chatColorMode(ChatColorMode.DEFAULT)
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new ChatCategoryColorPickerCustomMenuItemController(preferencesService);
    loadFxml("theme/chat/color_picker_menu_item.fxml", clazz -> instance, instance);
  }

  @Test
  public void testSetCurrentValue() {
    preferences.getChat().setGroupToColor(FXCollections.observableMap(Map.of(ChatUserCategory.FRIEND, Color.BLACK)));
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(ChatUserCategory.FRIEND);
    });
    assertEquals(Color.BLACK, instance.colorPicker.getValue());
  }

  @Test
  public void testChangeColorToAnother() {
    Map<ChatUserCategory, Color> colorMap = new HashMap<>();
    colorMap.put(ChatUserCategory.FRIEND, Color.BLACK);
    preferences.getChat().setGroupToColor(FXCollections.observableMap(colorMap));
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(ChatUserCategory.FRIEND);
      instance.colorPicker.setValue(Color.WHITE);
    });
    assertEquals(Color.WHITE, preferences.getChat().getGroupToColor().get(ChatUserCategory.FRIEND));
  }

  @Test
  public void testClearColor() {
    Map<ChatUserCategory, Color> colorMap = new HashMap<>();
    colorMap.put(ChatUserCategory.FRIEND, Color.BLACK);
    preferences.getChat().setGroupToColor(FXCollections.observableMap(colorMap));
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(ChatUserCategory.FRIEND);
      instance.colorPicker.setValue(null);
    });
    assertNull(preferences.getChat().getGroupToColor().get(ChatUserCategory.FRIEND));
  }

  @Test
  public void testVisibleItem() {
    runOnFxThreadAndWait(() -> instance.initialize());
    assertTrue(instance.getRoot().isVisible());
  }

  @Test
  public void testInvisibleItem() {
    preferences = PreferencesBuilder.create().chatPrefs().chatColorMode(ChatColorMode.RANDOM).then().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    runOnFxThreadAndWait(() -> instance.initialize());
    assertFalse(instance.getRoot().isVisible());
  }
}
