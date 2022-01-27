package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportPlayerMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  private ReportPlayerMenuItem instance;

  @BeforeEach
  public void setUp() {
    instance = new ReportPlayerMenuItem(i18n, uiService);
  }

  @Test
  public void testReportPlayer() {
    ReportDialogController mockController = mock(ReportDialogController.class);
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(mockController);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get();

    runOnFxThreadAndWait(() -> {
      ContextMenu contextMenu = new ContextMenu(instance);
      Label label = new Label();
      label.setContextMenu(contextMenu);
      getRoot().getChildren().add(label);

      instance.setObject(player);
      instance.onClicked();
    });

    verify(mockController).setOffender(player);
    verify(mockController).show();
  }

  @Test
  public void testVisibleItemIfPlayerIsNotSelf() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsSelf() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}