package com.faforever.client.fx.contextmenu.helper;

import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder.MenuItemBuilder;
import javafx.scene.control.ContextMenu;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ContextMenuBuilderHelper {

  public static ContextMenu mockContextMenuBuilderAndGetContextMenuMock(ContextMenuBuilder contextMenuBuilder) {
    MenuItemBuilder menuItemBuilder = mock(MenuItemBuilder.class, Answers.RETURNS_SELF);
    when(contextMenuBuilder.newBuilder()).thenReturn(menuItemBuilder);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(menuItemBuilder.build()).thenReturn(contextMenu);
    return contextMenu;
  }
}
