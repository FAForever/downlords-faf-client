package com.faforever.client.chat;

import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserListCustomizationControllerTest extends UITest {

  @Spy
  private ChatPrefs chatPrefs;

  @InjectMocks
  private UserListCustomizationController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/chat/user_list_customization.fxml", clazz -> instance);
  }

  @Test
  public void testCheckPropertiesListener() {
    boolean showMapNameCheckBoxSelected = instance.showMapNameCheckBox.isSelected();
    boolean showMapPreviewCheckBoxSelected = instance.showMapPreviewCheckBox.isSelected();

    runOnFxThreadAndWait(() -> instance.showMapNameCheckBox.fire());
    assertEquals(chatPrefs.showMapNameProperty().get(), !showMapNameCheckBoxSelected);

    runOnFxThreadAndWait(() -> instance.showMapPreviewCheckBox.fire());
    assertEquals(chatPrefs.showMapPreviewProperty().get(), !showMapPreviewCheckBoxSelected);
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}