package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatUserColorPickerCustomMenuItemControllerTest extends PlatformTest {
  private static final String CHANNEL_NAME = "testChannel";
  private static final String USERNAME = "junit";

  @Spy
  private ChatPrefs chatPrefs;
  @InjectMocks
  private ChatUserColorPickerCustomMenuItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatPrefs.setChatColorMode(ChatColorMode.DEFAULT);

    loadFxml("theme/chat/color_picker_menu_item.fxml", clazz -> instance, instance);
  }

  @Test
  public void testSetCurrentValue() {
    chatPrefs.setUserToColor(FXCollections.observableMap(Map.of(USERNAME, Color.BLACK)));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).get();
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(chatChannelUser);
    });
    assertEquals(Color.BLACK, instance.colorPicker.getValue());
    assertTrue(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testChangeColorToAnother() {
    Map<String, Color> colorMap = new HashMap<>();
    colorMap.put(USERNAME, Color.BLACK);
    chatPrefs.setUserToColor(FXCollections.observableMap(colorMap));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(chatChannelUser);
      instance.colorPicker.setValue(Color.WHITE);
    });
    assertEquals(Color.WHITE, chatPrefs.getUserToColor().get(USERNAME));
    assertEquals(Color.WHITE, chatChannelUser.getColor().orElseThrow());
    assertTrue(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testClearColor() {
    Map<String, Color> colorMap = new HashMap<>();
    colorMap.put(USERNAME, Color.BLACK);
    chatPrefs.setUserToColor(FXCollections.observableMap(colorMap));
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(chatChannelUser);
      instance.colorPicker.setValue(null);
    });
    assertNull(chatPrefs.getUserToColor().get(USERNAME));
    assertTrue(chatChannelUser.getColor().isEmpty());
    assertFalse(instance.removeCustomColorButton.isVisible());
  }

  @Test
  public void testVisibleItem() {
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).color(Color.BLACK).get();
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(chatChannelUser);
    });
    assertTrue(instance.getRoot().isVisible());
  }

  @Test
  public void testInvisibleItemWhenChatColorModeIsRandom() {
    ChatChannelUser chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).color(Color.BLACK).get();
    chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.setObject(chatChannelUser);
    });
    assertFalse(instance.getRoot().isVisible());
  }
}