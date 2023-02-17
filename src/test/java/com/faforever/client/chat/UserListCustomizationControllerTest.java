package com.faforever.client.chat;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserListCustomizationControllerTest extends UITest {

  @Mock
  private PreferencesService preferencesService;

  private ChatPrefs chatPrefs;

  @InjectMocks
  private UserListCustomizationController instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().chatPrefs().then().get();
    chatPrefs = preferences.getChat();
    Mockito.when(preferencesService.getPreferences()).thenReturn(preferences);

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