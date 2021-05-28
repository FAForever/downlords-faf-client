package com.faforever.client.chat;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ChatCategoryContextMenuControllerTest extends AbstractPlainJavaFxTest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private PreferencesService preferencesService;

  private Preferences preferences;
  private ChatCategoryContextMenuController instance;
  private ChatUserCategory chatUserCategory;

  @Before
  public void setUp() throws Exception {
    instance = new ChatCategoryContextMenuController(preferencesService);

    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);

    loadFxml("theme/chat/chat_category_context_menu.fxml", clazz -> instance);

    chatUserCategory = ChatUserCategory.FRIEND;
  }

  @Test
  public void testChangePlayerColor() {
    instance.setCategory(chatUserCategory);

    instance.colorPicker.setValue(Color.ALICEBLUE);

    assertEquals(Color.ALICEBLUE, preferences.getChat().getGroupToColor().get(chatUserCategory));
  }
}
