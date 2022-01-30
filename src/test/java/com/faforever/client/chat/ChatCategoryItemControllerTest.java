package com.faforever.client.chat;

import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatCategoryItemControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private ChatCategoryItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/chat/chat_user_category.fxml", clazz -> instance);
  }

  @Test
  public void testOnContextMenuRequested() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);

    instance.onContextMenuRequested(mock(ContextMenuEvent.class));
    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }
}