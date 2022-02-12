package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserColorPickerCustomMenuItemControllerTest extends UITest {

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private EventBus eventBus;

  private Preferences preferences;
  @InjectMocks
  private ChatUserColorPickerCustomMenuItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().defaultValues().chatPrefs()
        .chatColorMode(ChatColorMode.DEFAULT)
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    loadFxml("theme/chat/color_picker_menu_item.fxml", clazz -> instance, instance);
  }

  @Test
  public void testSetCurrentValue() {
    preferences.getChat().setUserToColor(FXCollections.observableMap(Map.of("junit", Color.BLACK)));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create("junit").get();
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(chatChannelUser);
    });
    assertEquals(Color.BLACK, instance.colorPicker.getValue());
    assertTrue(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testChangeColorToAnother() {
    Map<String, Color> colorMap = new HashMap<>();
    colorMap.put("junit", Color.BLACK);
    preferences.getChat().setUserToColor(FXCollections.observableMap(colorMap));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create("junit").color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(chatChannelUser);
      instance.colorPicker.setValue(Color.WHITE);
    });
    assertEquals(Color.WHITE, preferences.getChat().getUserToColor().get("junit"));
    assertEquals(Color.WHITE, chatChannelUser.getColor().orElseThrow());
    assertTrue(instance.removeCustomColorButton.isVisible());
    verify(eventBus).post(any(ChatUserColorChangeEvent.class));
  }

  @Test
  public void testClearColor() {
    Map<String, Color> colorMap = new HashMap<>();
    colorMap.put("junit", Color.BLACK);
    preferences.getChat().setUserToColor(FXCollections.observableMap(colorMap));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create("junit").color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(chatChannelUser);
      instance.colorPicker.setValue(null);
    });
    assertNull(preferences.getChat().getUserToColor().get("junit"));
    assertTrue(chatChannelUser.getColor().isEmpty());
    assertFalse(instance.removeCustomColorButton.isVisible());
    verify(eventBus).post(any(ChatUserColorChangeEvent.class));
  }

  @Test
  public void testVisibleItem() {
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create("junit").color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(chatChannelUser);
    });
    assertTrue(instance.getRoot().isVisible());
  }

  @Test
  public void testInvisibleItemWhenChatColorModeIsRandom() {
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create("junit").color(Color.BLACK).get();
    preferences = PreferencesBuilder.create().chatPrefs().chatColorMode(ChatColorMode.RANDOM).then().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    runOnFxThreadAndWait(() -> {
      instance.initialize();
      instance.setObject(chatChannelUser);
    });
    assertFalse(instance.getRoot().isVisible());
  }
}