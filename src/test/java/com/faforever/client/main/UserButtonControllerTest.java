package com.faforever.client.main;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserButtonControllerTest extends UITest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ReportDialogController reportDialogController;
  @Mock
  private PlayerInfoWindowController playerInfoWindowController;
  @Mock
  private UserService userService;


  @InjectMocks
  private UserButtonController instance;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(playerInfoWindowController);

    player = PlayerBeanBuilder.create().defaultValues().username(TEST_USER_NAME).get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);

    loadFxml("theme/user_button.fxml", clazz -> instance);
  }

  @Test
  public void testOnLoginSuccess() {
    instance.onLoginSuccessEvent(new LoginSuccessEvent());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(TEST_USER_NAME, instance.userMenuButtonRoot.getText());
  }

  @Test
  public void testOnGetRoot() {
    assertEquals(instance.userMenuButtonRoot, instance.getRoot());
  }

  @Test
  public void testShowProfile() {
    instance.onShowProfile(null);

    verify(playerInfoWindowController).setPlayer(player);
    verify(playerInfoWindowController).show();
  }

  @Test
  public void testReport() {
    instance.onReport(null);

    verify(reportDialogController).setAutoCompleteWithOnlinePlayers();
    verify(reportDialogController).show();
  }

  @Test
  public void testLogOut() {
    instance.onLogOut(null);

    verify(eventBus).post(any(LogOutRequestEvent.class));
  }

}

