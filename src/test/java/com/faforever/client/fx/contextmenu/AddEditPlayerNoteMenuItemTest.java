package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerNoteController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddEditPlayerNoteMenuItemTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;

  @InjectMocks
  private AddEditPlayerNoteMenuItem instance;

  @Override
  protected Pane getRoot() {
    return new StackPane();
  }

  @Test
  public void testOnItemClicked() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    PlayerNoteController controllerMock = mock(PlayerNoteController.class);
    Dialog dialogMock = mock(Dialog.class);
    when(uiService.loadFxml("theme/player_note.fxml")).thenReturn(controllerMock);
    when(uiService.showInDialog(any(), any())).thenReturn(dialogMock);

    runOnFxThreadAndWait(() -> {
      ContextMenu contextMenu = new ContextMenu(instance);
      Label label = new Label();
      label.setContextMenu(contextMenu);
      getRoot().getChildren().add(label);
      contextMenu.show(getStage());
      instance.setObject(player);
      instance.onClicked();
    });

    verify(dialogMock).show();
  }

  @Test
  public void testAddNoteText() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(i18n.get("chat.userContext.addNote")).thenReturn("add");

    instance.setObject(player);
    assertEquals("add", instance.getText());
  }

  @Test
  public void testEditNoteText() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit").get();
    when(i18n.get("chat.userContext.editNote")).thenReturn("edit");

    instance.setObject(player);
    assertEquals("edit", instance.getText());
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItem() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfOwnPlayer() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.setObject(player);
    assertFalse(instance.isVisible());
  }
}