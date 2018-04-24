package com.faforever.client.fx;

import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WindowControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  UiService uiService;
  @Mock
  GameService gameService;
  @Mock
  NotificationService notificationService;
  @Mock
  I18n i18n;


  private WindowController instance;


  @Before
  public void setUp() {

    instance = new WindowController(uiService, gameService, notificationService, i18n);

  }

  @Test
  public void testOnCloseButtonClickedFARunning() {
    when(gameService.isForgedAllianceProcessRunning()).thenReturn(true);
    when(i18n.get("exitWarning.title")).thenReturn("exitWarningTitle");
    when(i18n.get("exitWarning.message")).thenReturn("exitWarningMessage");
    when(i18n.get("yes")).thenReturn("yes");
    when(i18n.get("no")).thenReturn("no");

    instance.onCloseButtonClicked();

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    assertEquals(notification.getTitle(), "exitWarningTitle");
    assertEquals(notification.getText(), "exitWarningMessage");
    assertEquals(notification.getSeverity(), Severity.WARN);
    assertEquals(notification.getActions().size(), 2);
    assertEquals(notification.getActions().get(0).getTitle(), "yes");
    assertEquals(notification.getActions().get(1).getTitle(), "no");
  }


}
