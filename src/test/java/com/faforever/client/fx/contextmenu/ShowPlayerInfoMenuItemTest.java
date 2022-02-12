package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShowPlayerInfoMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @InjectMocks
  private ShowPlayerInfoMenuItem instance;

  @Test
  public void testShowPlayerInfo() {
    PlayerInfoWindowController mockController = mock(PlayerInfoWindowController.class);
    when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(mockController);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get();

    runOnFxThreadAndWait(() -> {
      ContextMenu contextMenu = new ContextMenu(instance);
      Label label = new Label();
      label.setContextMenu(contextMenu);
      getRoot().getChildren().add(label);

      instance.setObject(player);
      instance.onClicked();
    });

    verify(mockController).setPlayer(player);
    verify(mockController).show();
  }

  @Test
  public void testVisibleItemIfNonNullPlayer() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemIfPlayerIsSelf() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}