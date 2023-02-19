package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ChangeUsernameColorMenuItemTest extends UITest {

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Spy
  private ChatPrefs chatPrefs;

  @InjectMocks
  private ChangeUsernameColorMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatPrefs.setChatColorMode(ChatColorMode.DEFAULT);
  }

  @Test
  public void testOnClicked() {
    runOnFxThreadAndWait(() -> {
      ContextMenu contextMenu = new ContextMenu(instance);
      Label label = new Label();
      label.setContextMenu(contextMenu);
      getRoot().getChildren().add(label);
      instance.setObject(ChatChannelUserBuilder.create("junit", "channel").get());
      contextMenu.show(getRoot(), 0, 0);
    });

    verifyNoInteractions(contextMenuBuilder);
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);
    instance.onClicked();
    verify(contextMenuMock).show(eq(getStage()), anyDouble(), anyDouble());
  }

  @Test
  public void testVisibleItem() {
    runOnFxThreadAndWait(() -> instance.setObject(ChatChannelUserBuilder.create("junit", "channel").get()));
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenChatColorModeIsRandom() {
    chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
    runOnFxThreadAndWait(() -> instance.setObject(new ChatChannelUser("test", "test")));
    assertFalse(instance.isVisible());
  }
}